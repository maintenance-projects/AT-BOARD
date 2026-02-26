package kr.co.ultari.at_board.scheduler;

import kr.co.ultari.at_board.model.primary.BoardAttachment;
import kr.co.ultari.at_board.repository.primary.BoardAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 만료된 첨부파일의 물리 파일을 매일 새벽 3시에 삭제하는 스케줄러.
 * DB 레코드는 유지하여 "다운로드 기간 만료" 표시에 사용.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AttachmentRetentionScheduler {

    private final BoardAttachmentRepository attachmentRepository;

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredAttachments() {
        LocalDateTime now = LocalDateTime.now();
        List<BoardAttachment> expired = attachmentRepository.findExpiredWithFile(now);

        if (expired.isEmpty()) {
            log.debug("만료된 첨부파일 없음");
            return;
        }

        log.info("=== 만료 첨부파일 삭제 시작: {}건 ===", expired.size());
        int deleted = 0;
        int notFound = 0;

        for (BoardAttachment attachment : expired) {
            try {
                Path filePath = Paths.get(uploadPath, attachment.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    deleted++;
                    log.debug("삭제 완료: {}", attachment.getFilePath());
                } else {
                    notFound++;
                }
            } catch (IOException e) {
                log.error("파일 삭제 실패 (id={}): {}", attachment.getId(), e.getMessage());
            }
        }

        log.info("=== 만료 첨부파일 삭제 완료: 삭제 {}건, 파일 없음 {}건 ===", deleted, notFound);
    }
}
