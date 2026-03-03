package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.model.primary.BoardAttachment;
import kr.co.ultari.at_board.service.AppSettingService;
import kr.co.ultari.at_board.service.BoardAttachmentService;
import kr.co.ultari.at_board.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@Slf4j
@RequiredArgsConstructor
public class FileController {

    private final BoardAttachmentService boardAttachmentService;
    private final AppSettingService appSettingService;
    private final FileStorageService fileStorageService;

    @Value("${file.upload.max-size:10485760}")
    private long maxFileSize;

    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    private static final String[] ALLOWED_MIME_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 로그인 체크 (일반 사용자 또는 관리자)
            if (session.getAttribute("currentUser") == null && session.getAttribute("adminUser") == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(error);
            }

            // 파일 검증
            if (file.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "파일이 비어있습니다.");
                return ResponseEntity.badRequest().body(error);
            }

            // 파일 크기 검증
            if (file.getSize() > maxFileSize) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "파일 크기는 " + (maxFileSize / 1024 / 1024) + "MB를 초과할 수 없습니다.");
                return ResponseEntity.badRequest().body(error);
            }

            // MIME 타입 검증
            String contentType = file.getContentType();
            boolean validMimeType = false;
            for (String allowedType : ALLOWED_MIME_TYPES) {
                if (allowedType.equals(contentType)) {
                    validMimeType = true;
                    break;
                }
            }
            if (!validMimeType) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "지원하지 않는 파일 형식입니다. (JPG, PNG, GIF, WEBP만 가능)");
                return ResponseEntity.badRequest().body(error);
            }

            // 파일 확장자 검증
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            boolean validExtension = false;
            for (String allowedExt : ALLOWED_EXTENSIONS) {
                if (allowedExt.equals(extension)) {
                    validExtension = true;
                    break;
                }
            }
            if (!validExtension) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "지원하지 않는 파일 확장자입니다.");
                return ResponseEntity.badRequest().body(error);
            }

            // 저장 경로 생성 (날짜별 폴더 + 고유 파일명)
            String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String uniqueFilename = UUID.randomUUID().toString() + extension;
            String relativePath = "imgs/" + dateFolder + "/" + uniqueFilename;

            // 파일 저장 (로컬 또는 원격)
            String webPath = fileStorageService.uploadImage(file, relativePath);

            response.put("url", webPath);
            response.put("filename", originalFilename);
            log.info("Image uploaded: {}", webPath);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to upload image", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "파일 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/attachment")
    public ResponseEntity<Map<String, Object>> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        if (session.getAttribute("currentUser") == null && session.getAttribute("adminUser") == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        try {
            boolean isAdmin = session.getAttribute("adminUser") != null;
            // calculateExpiresAt은 트랜잭션 밖(uploadAttachment 트랜잭션 시작 전)에서 호출
            // → 두 트랜잭션이 커넥션을 동시에 점유하지 않고 순차 사용 (커넥션 풀 안전)
            LocalDateTime expiresAt = isAdmin ? null : appSettingService.calculateExpiresAt(file.getSize());
            BoardAttachment attachment = isAdmin
                    ? boardAttachmentService.uploadAttachmentAdmin(file)
                    : boardAttachmentService.uploadAttachment(file, expiresAt);
            response.put("id", attachment.getId());
            response.put("originalName", attachment.getOriginalName());
            response.put("fileSize", attachment.getFileSize());
            response.put("fileSizeDisplay", attachment.getFileSizeDisplay());
            response.put("mimeType", attachment.getMimeType());
            if (attachment.getExpiresAt() != null) {
                response.put("expiresAt", attachment.getExpiresAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
            log.info("Attachment uploaded: {} ({}) isAdmin={} expiresAt={}", attachment.getOriginalName(), attachment.getId(), isAdmin, attachment.getExpiresAt());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IOException e) {
            log.error("Failed to upload attachment", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "파일 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
