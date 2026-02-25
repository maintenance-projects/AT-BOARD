package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.AppSetting;
import kr.co.ultari.at_board.repository.primary.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppSettingService {

    private final AppSettingRepository settingRepository;

    public static final String KEY_MAX_ATTACHMENT_SIZE = "file.attachment.max-size";
    public static final String KEY_BLOCKED_EXTENSIONS  = "file.attachment.blocked-extensions";

    private static final long   DEFAULT_MAX_SIZE           = 52428800L; // 50MB
    private static final String DEFAULT_BLOCKED_EXTENSIONS = "exe,sh,bat,cmd,ps1,vbs,msi,dll,com,scr,pif,reg";

    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public long getMaxAttachmentSize() {
        AppSetting setting = settingRepository.findById(KEY_MAX_ATTACHMENT_SIZE).orElse(null);
        if (setting == null || setting.getValue() == null || setting.getValue().trim().isEmpty()) {
            log.debug("getMaxAttachmentSize: no DB setting found, using default {}MB", DEFAULT_MAX_SIZE / 1024 / 1024);
            return DEFAULT_MAX_SIZE;
        }
        try {
            long size = Long.parseLong(setting.getValue().trim());
            log.debug("getMaxAttachmentSize: {}MB (from DB)", size / 1024 / 1024);
            return size;
        } catch (NumberFormatException e) {
            log.warn("getMaxAttachmentSize: invalid value '{}', using default", setting.getValue());
            return DEFAULT_MAX_SIZE;
        }
    }

    @Transactional("primaryTransactionManager")
    public void setMaxAttachmentSize(long sizeInBytes) {
        settingRepository.save(new AppSetting(KEY_MAX_ATTACHMENT_SIZE, String.valueOf(sizeInBytes)));
    }

    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public Set<String> getBlockedExtensions() {
        AppSetting setting = settingRepository.findById(KEY_BLOCKED_EXTENSIONS).orElse(null);
        String raw = (setting != null && setting.getValue() != null)
                ? setting.getValue()
                : DEFAULT_BLOCKED_EXTENSIONS;
        log.debug("getBlockedExtensions: dbValue='{}', raw='{}'",
                setting != null ? setting.getValue() : "(no record)", raw);
        if (raw.trim().isEmpty()) return new LinkedHashSet<>();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional("primaryTransactionManager")
    public void setBlockedExtensions(Set<String> extensions) {
        String value = extensions.stream()
                .map(String::toLowerCase)
                .collect(Collectors.joining(","));
        settingRepository.save(new AppSetting(KEY_BLOCKED_EXTENSIONS, value));
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
    }

    @Transactional("primaryTransactionManager")
    public void removeBlockedExtension(String ext) {
        String cleaned = ext.toLowerCase().trim().replaceAll("^\\.", "");
        Set<String> exts = readBlockedExtensionsFromDb();
        exts.remove(cleaned);
        String value = String.join(",", exts);
        log.info("removeBlockedExtension: removing '{}', saving '{}'", cleaned, value);
        settingRepository.save(new AppSetting(KEY_BLOCKED_EXTENSIONS, value));
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
}
