package kr.co.ultari.at_board.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ultari.at_board.dto.notification.MsgNotiRequestDto;
import kr.co.ultari.at_board.dto.notification.NotiReceiverType;
import kr.co.ultari.at_board.dto.notification.NotiReceiverVO;
import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.secondary.Dept;
import kr.co.ultari.at_board.repository.secondary.DepartmentRepository;
import kr.co.ultari.at_board.repository.secondary.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    @Value("${notification.server.type:none}")
    private String serverType;

    @Value("${notification.fmc.url:}")
    private String fmcUrl;

    @Value("${notification.msg.url:}")
    private String msgUrl;

    @Value("${notification.msg.system-code:ATBOARD}")
    private String systemCode;

    @Value("${notification.server.base-url:}")
    private String serverBaseUrl;

    @Value("${notification.msg.link-url-enabled:true}")
    private boolean linkUrlEnabled;

    @Value("${notification.msg.request-delay-ms:100}")
    private long requestDelayMs;

    @Value("${notification.msg.persistence:true}")
    private boolean persistence;

    private static final DateTimeFormatter SEND_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DepartmentService departmentService;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 새 게시글 알림 발송 진입점. 비동기 실행으로 게시글 저장에 영향 없음.
     * 카테고리의 notificationEnabled가 false이거나 serverType이 none이면 즉시 반환.
     */
    @Async
    public void notifyNewPost(Board board) {
        try {
            BoardCategory category = board.getCategory();
            if (category == null || !Boolean.TRUE.equals(category.getNotificationEnabled())) return;
            if ("none".equalsIgnoreCase(serverType)) return;

            if ("fmc".equalsIgnoreCase(serverType)) {
                Set<String> deptCodes = buildTargetDeptCodes(category);
                sendToFmc(board, deptCodes);
            } else if ("msg".equalsIgnoreCase(serverType)) {
                List<String> userIds = buildTargetUserIds(category).stream()
                        .filter(uid -> !uid.equals(board.getUserId()))
                        .collect(Collectors.toList());
                sendToMsg(board, userIds);
            } else {
                log.warn("[알림] 알 수 없는 serverType: {}", serverType);
            }
        } catch (Exception e) {
            log.error("[알림] 알림 처리 중 오류 발생 - boardId={}", board.getId(), e);
        }
    }

    /**
     * MSG용 수신 사용자 ID 목록 구성.
     * - 전사 게시판: 전체 사용자
     * - 부서 게시판: base 부서 + 모든 하위 부서 소속 사용자
     */
    private List<String> buildTargetUserIds(BoardCategory category) {
        if (isCompanyWide(category)) {
            return userRepository.findAllUserIds();
        }
        Set<String> allDeptIds = departmentService.getSelfAndAllDescendantDeptIds(getBaseDeptIds(category));
        return userRepository.findUserIdsByDeptIdIn(allDeptIds);
    }

    /**
     * FMC용 수신 부서 코드 목록 구성 (하위 부서까지 직접 전개).
     * - 전사 게시판: 전체 부서 코드
     * - 부서 게시판: base 부서 + 모든 하위 부서 코드
     */
    private Set<String> buildTargetDeptCodes(BoardCategory category) {
        if (isCompanyWide(category)) {
            return departmentRepository.findAll().stream()
                    .map(Dept::getDeptId)
                    .collect(Collectors.toSet());
        }
        return departmentService.getSelfAndAllDescendantDeptIds(getBaseDeptIds(category));
    }

    private boolean isCompanyWide(BoardCategory category) {
        String deptId = category.getDeptId();
        boolean hasDeptId = deptId != null && !deptId.trim().isEmpty();
        boolean hasDeptIds = category.getDeptIds() != null && !category.getDeptIds().isEmpty();
        return !hasDeptId && !hasDeptIds;
    }

    private Set<String> getBaseDeptIds(BoardCategory category) {
        Set<String> base = new HashSet<>();
        if (category.getDeptId() != null && !category.getDeptId().trim().isEmpty()) {
            base.add(category.getDeptId());
        }
        if (category.getDeptIds() != null) {
            base.addAll(category.getDeptIds());
        }
        return base;
    }

    /**
     * FMC 서버 전송 (stub).
     * 추후 HTTP 전송 구현 예정.
     */
    private void sendToFmc(Board board, Set<String> deptCodes) {
        log.info("[알림-FMC] 전송 준비 - boardId={}, categoryId={}, title={}, 수신부서수={}, url={}",
                board.getId(), board.getCategory().getId(), board.getTitle(), deptCodes.size(), fmcUrl);
        log.debug("[알림-FMC] 수신부서={}", deptCodes);
        // TODO: fmcUrl로 HTTP POST 전송 구현
    }

    /**
     * MSG 서버 전송 (USER 방식).
     * - linkUrlEnabled=true : 사용자별 개인화 SSO linkUrl 포함, 1건씩 전송 (딜레이 적용)
     * - linkUrlEnabled=false : linkUrl 없이 전체 사용자를 단일 요청으로 전송
     */
    private void sendToMsg(Board board, List<String> userIds) {
        if (userIds.isEmpty()) {
            log.info("[알림-MSG] 수신 사용자 없음 - boardId={}", board.getId());
            return;
        }
        if (msgUrl == null || msgUrl.trim().isEmpty()) {
            log.warn("[알림-MSG] msgUrl이 설정되지 않음");
            return;
        }

        String categoryName = board.getCategory().getName();
        String authorName = board.getAuthorNameOnly();
        String boardId = String.valueOf(board.getId());
        String categoryId = String.valueOf(board.getCategory().getId());
        String sendDate = LocalDateTime.now().format(SEND_DATE_FMT);
        String notiTitle = authorName + " 님이 [" + categoryName + "] 에 새 글을 작성 하였습니다.";
        String notiContents = authorName + " [" + board.getTitle() + "]";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (!linkUrlEnabled) {
            // linkUrl 불필요 → 전체 사용자 단일 요청
            NotiReceiverVO receiver = NotiReceiverVO.builder()
                    .list(userIds)
                    .type(NotiReceiverType.USER)
                    .build();
            MsgNotiRequestDto requestDto = MsgNotiRequestDto.builder()
                    .notiCode(UUID.randomUUID().toString())
                    .senderName(categoryName)
                    .systemCode(systemCode)
                    .notiTitle(notiTitle)
                    .notiContents(notiContents)
                    .linkUrl("")
                    .sendDate(sendDate)
                    .alertType("0")
                    .persistence(persistence)
                    .receivers(Collections.singletonList(receiver))
                    .build();
            try {
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                log.info("[알림-MSG] 단일 요청 전송 - boardId={}, 수신자수={}, body={}", boardId, userIds.size(), jsonBody);
                HttpEntity<String> httpEntity = new HttpEntity<>(jsonBody, headers);
                String response = restTemplate.postForObject(msgUrl, httpEntity, String.class);
                log.info("[알림-MSG] 전송 완료 - boardId={}, 수신자수={}, response={}", boardId, userIds.size(), response);
            } catch (Exception e) {
                log.error("[알림-MSG] 전송 실패 - boardId={}, url={}", boardId, msgUrl, e);
            }
            return;
        }

        // linkUrlEnabled=true → 사용자별 개인화 linkUrl 포함, 딜레이 적용
        String baseUrl = serverBaseUrl != null ? serverBaseUrl.trim() : "";
        int successCount = 0;
        int total = userIds.size();
        for (int i = 0; i < total; i++) {
            String userId = userIds.get(i);
            String linkUrl = baseUrl.isEmpty() ? ""
                    : baseUrl + "/" + userId + "/board/" + boardId + "?categoryId=" + categoryId;

            NotiReceiverVO receiver = NotiReceiverVO.builder()
                    .list(Collections.singletonList(userId))
                    .type(NotiReceiverType.USER)
                    .build();
            MsgNotiRequestDto requestDto = MsgNotiRequestDto.builder()
                    .notiCode(UUID.randomUUID().toString())
                    .senderName(categoryName)
                    .systemCode(systemCode)
                    .notiTitle(notiTitle)
                    .notiContents(notiContents)
                    .linkUrl(linkUrl)
                    .sendDate(sendDate)
                    .alertType("0")
                    .persistence(persistence)
                    .receivers(Collections.singletonList(receiver))
                    .build();
            try {
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                HttpEntity<String> httpEntity = new HttpEntity<>(jsonBody, headers);
                restTemplate.postForObject(msgUrl, httpEntity, String.class);
                successCount++;
            } catch (org.springframework.web.client.ResourceAccessException e) {
                log.error("[알림-MSG] 서버 연결 실패(타임아웃/네트워크), 이후 전송 중단 - boardId={}, userId={}", boardId, userId, e);
                break;
            } catch (Exception e) {
                log.error("[알림-MSG] 전송 실패 - boardId={}, userId={}", boardId, userId, e);
            }

            if (requestDelayMs > 0 && i < total - 1) {
                try {
                    Thread.sleep(requestDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("[알림-MSG] 딜레이 중단 - boardId={}", boardId);
                    break;
                }
            }
        }
        log.info("[알림-MSG] 전송 완료 - boardId={}, 수신자={}/{}", boardId, successCount, total);
    }


}