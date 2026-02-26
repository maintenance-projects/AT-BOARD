package kr.co.ultari.at_board.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ultari.at_board.model.primary.AppSetting;
import kr.co.ultari.at_board.model.primary.AttachmentRetentionRule;
import kr.co.ultari.at_board.repository.primary.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppSettingService {

    private final AppSettingRepository settingRepository;

    public static final String KEY_MAX_ATTACHMENT_SIZE = "file.attachment.max-size";
    public static final String KEY_BLOCKED_EXTENSIONS  = "file.attachment.blocked-extensions";
    public static final String KEY_RETENTION_RULES     = "attachment.retention.rules";

    private static final long   DEFAULT_MAX_SIZE           = 52428800L; // 50MB
    private static final String DEFAULT_BLOCKED_EXTENSIONS = "exe,sh,bat,cmd,ps1,vbs,msi,dll,com,scr,pif,reg";

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 인-메모리 캐시 (파일 업로드마다 DB 조회 방지)
    private volatile long cachedMaxSize = -1L;
    private volatile Set<String> cachedBlockedExtensions = null;
    private volatile List<AttachmentRetentionRule> cachedRetentionRules = null;

    public long getMaxAttachmentSize() {
        long cached = cachedMaxSize;
        if (cached >= 0) return cached;
        AppSetting setting = settingRepository.findById(KEY_MAX_ATTACHMENT_SIZE).orElse(null);
        if (setting == null || setting.getValue() == null || setting.getValue().trim().isEmpty()) {
            log.debug("getMaxAttachmentSize: no DB setting found, using default {}MB", DEFAULT_MAX_SIZE / 1024 / 1024);
            cachedMaxSize = DEFAULT_MAX_SIZE;
            return DEFAULT_MAX_SIZE;
        }
        try {
            long size = Long.parseLong(setting.getValue().trim());
            log.debug("getMaxAttachmentSize: {}MB (from DB)", size / 1024 / 1024);
            cachedMaxSize = size;
            return size;
        } catch (NumberFormatException e) {
            log.warn("getMaxAttachmentSize: invalid value '{}', using default", setting.getValue());
            cachedMaxSize = DEFAULT_MAX_SIZE;
            return DEFAULT_MAX_SIZE;
        }
    }

    @Transactional("primaryTransactionManager")
    public void setMaxAttachmentSize(long sizeInBytes) {
        settingRepository.save(new AppSetting(KEY_MAX_ATTACHMENT_SIZE, String.valueOf(sizeInBytes)));
        cachedMaxSize = sizeInBytes;
    }

    public Set<String> getBlockedExtensions() {
        Set<String> cached = cachedBlockedExtensions;
        if (cached != null) return cached;
        AppSetting setting = settingRepository.findById(KEY_BLOCKED_EXTENSIONS).orElse(null);
        String raw = (setting != null && setting.getValue() != null)
                ? setting.getValue()
                : DEFAULT_BLOCKED_EXTENSIONS;
        log.debug("getBlockedExtensions: dbValue='{}', raw='{}'",
                setting != null ? setting.getValue() : "(no record)", raw);
        Set<String> result;
        if (raw.trim().isEmpty()) {
            result = new LinkedHashSet<>();
        } else {
            result = Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        cachedBlockedExtensions = result;
        return result;
    }

    @Transactional("primaryTransactionManager")
    public void setBlockedExtensions(Set<String> extensions) {
        String value = extensions.stream()
                .map(String::toLowerCase)
                .collect(Collectors.joining(","));
        settingRepository.save(new AppSetting(KEY_BLOCKED_EXTENSIONS, value));
        cachedBlockedExtensions = null;
    }

    @Transactional("primaryTransactionManager")
    public void addBlockedExtension(String ext) {
        String cleaned = ext.toLowerCase().trim().replaceAll("^\\.", "");
        if (cleaned.isEmpty()) return;
        Set<String> exts = readBlockedExtensionsFromDb();
        exts.add(cleaned);
        String value = String.join(",", exts);
        log.info("addBlockedExtension: adding '{}', saving '{}'", cleaned, value);
        settingRepository.save(new AppSetting(KEY_BLOCKED_EXTENSIONS, value));
        cachedBlockedExtensions = null;
    }

    @Transactional("primaryTransactionManager")
    public void removeBlockedExtension(String ext) {
        String cleaned = ext.toLowerCase().trim().replaceAll("^\\.", "");
        Set<String> exts = readBlockedExtensionsFromDb();
        exts.remove(cleaned);
        String value = String.join(",", exts);
        log.info("removeBlockedExtension: removing '{}', saving '{}'", cleaned, value);
        settingRepository.save(new AppSetting(KEY_BLOCKED_EXTENSIONS, value));
        cachedBlockedExtensions = null;
    }

    /** self-invocation 문제를 피하기 위해 DB에서 직접 읽는 내부 헬퍼 */
    private Set<String> readBlockedExtensionsFromDb() {
        AppSetting setting = settingRepository.findById(KEY_BLOCKED_EXTENSIONS).orElse(null);
        String raw = (setting != null && setting.getValue() != null)
                ? setting.getValue()
                : DEFAULT_BLOCKED_EXTENSIONS;
        if (raw.trim().isEmpty()) return new LinkedHashSet<>();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // ── 첨부파일 보관 기간 규칙 ──────────────────────────────────────────────────

    public List<AttachmentRetentionRule> getRetentionRules() {
        List<AttachmentRetentionRule> cached = cachedRetentionRules;
        if (cached != null) return cached;
        List<AttachmentRetentionRule> rules = readRetentionRulesFromDb();
        cachedRetentionRules = rules;
        return rules;
    }

    @Transactional("primaryTransactionManager")
    public void addRetentionRule(int sizeThresholdMb, int retentionDays) {
        List<AttachmentRetentionRule> rules = new ArrayList<>(readRetentionRulesFromDb());
        // 같은 threshold가 있으면 덮어쓰기
        rules.removeIf(r -> r.getSizeThresholdMb() == sizeThresholdMb);
        rules.add(new AttachmentRetentionRule(sizeThresholdMb, retentionDays));
        // threshold 내림차순 정렬 (큰 용량 기준 먼저)
        rules.sort(Comparator.comparingInt(AttachmentRetentionRule::getSizeThresholdMb).reversed());
        saveRetentionRules(rules);
        cachedRetentionRules = null;
    }

    @Transactional("primaryTransactionManager")
    public void removeRetentionRule(int sizeThresholdMb) {
        List<AttachmentRetentionRule> rules = new ArrayList<>(readRetentionRulesFromDb());
        rules.removeIf(r -> r.getSizeThresholdMb() == sizeThresholdMb);
        saveRetentionRules(rules);
        cachedRetentionRules = null;
    }

    /**
     * 파일 크기에 해당하는 만료일 계산.
     * 규칙 중 fileSize >= threshold 를 만족하는 것 중 가장 큰 threshold 규칙 적용.
     * @return 만료 일시, null = 무기한
     */
    public LocalDateTime calculateExpiresAt(long fileSizeBytes) {
        List<AttachmentRetentionRule> rules = getRetentionRules();
        if (rules.isEmpty()) {
            log.info("calculateExpiresAt: DB에 보관 기간 규칙 없음 → 무기한 보관");
            return null;
        }

        long fileSizeMb = fileSizeBytes / (1024L * 1024);
        log.info("calculateExpiresAt: 파일 크기 {}bytes = {}MB, 규칙 {}개 → {}", fileSizeBytes, fileSizeMb, rules.size(), rules);

        LocalDateTime now = LocalDateTime.now();
        AttachmentRetentionRule applicable = null;
        for (AttachmentRetentionRule rule : rules) {
            if (fileSizeMb >= rule.getSizeThresholdMb()) {
                if (applicable == null || rule.getSizeThresholdMb() > applicable.getSizeThresholdMb()) {
                    applicable = rule;
                }
            }
        }
        if (applicable == null || applicable.getRetentionDays() <= 0) {
            log.info("calculateExpiresAt: {}MB 파일에 매칭 규칙 없음 → 무기한 보관", fileSizeMb);
            return null;
        }
        LocalDateTime expiresAt = now.plusDays(applicable.getRetentionDays());
        log.info("calculateExpiresAt: {}MB 파일 → {}MB 이상 규칙 적용 → {}일 보관, 만료일 {}",
                fileSizeMb, applicable.getSizeThresholdMb(), applicable.getRetentionDays(), expiresAt);
        return expiresAt;
    }

    private List<AttachmentRetentionRule> readRetentionRulesFromDb() {
        AppSetting setting = settingRepository.findById(KEY_RETENTION_RULES).orElse(null);
        if (setting == null || setting.getValue() == null || setting.getValue().trim().isEmpty()) {
            log.info("readRetentionRulesFromDb: app_settings에 '{}' 키 없음 (setting={})", KEY_RETENTION_RULES, setting);
            return new ArrayList<>();
        }
        log.info("readRetentionRulesFromDb: value={}", setting.getValue());
        try {
            List<AttachmentRetentionRule> rules = new ArrayList<>(objectMapper.readValue(
                    setting.getValue(), new TypeReference<List<AttachmentRetentionRule>>() {}));
            log.info("readRetentionRulesFromDb: 파싱 성공 - {}개 규칙", rules.size());
            return rules;
        } catch (Exception e) {
            log.error("보관 기간 규칙 파싱 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveRetentionRules(List<AttachmentRetentionRule> rules) {
        try {
            String json = objectMapper.writeValueAsString(rules);
            settingRepository.save(new AppSetting(KEY_RETENTION_RULES, json));
        } catch (Exception e) {
            log.error("보관 기간 규칙 저장 실패: {}", e.getMessage());
        }
    }
}
