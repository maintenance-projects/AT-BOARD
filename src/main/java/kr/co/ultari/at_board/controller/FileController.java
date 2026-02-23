package kr.co.ultari.at_board.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@Slf4j
public class FileController {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    @Value("${file.upload.max-size:10485760}") // 기본 10MB
    private long maxFileSize;

    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    private static final String[] ALLOWED_MIME_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    /**
     * 애플리케이션 시작 시 업로드 폴더 생성
     */
    @PostConstruct
    public void init() {
        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Upload directory created: {}", uploadDir.toAbsolutePath());
            } else {
                log.info("Upload directory already exists: {}", uploadDir.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", uploadPath, e);
        }
    }

    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 로그인 체크
            if (session.getAttribute("currentUser") == null) {
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

            // 업로드 디렉토리 생성 (날짜별 폴더)
            String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path uploadDir = Paths.get(uploadPath, dateFolder);
            Files.createDirectories(uploadDir);

            // 고유 파일명 생성
            String uniqueFilename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadDir.resolve(uniqueFilename);

            // 파일 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 웹 경로 생성
            String webPath = "/uploads/" + dateFolder + "/" + uniqueFilename;

            response.put("url", webPath);
            response.put("filename", originalFilename);
            log.info("Image uploaded: {}", webPath);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to upload image", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "파일 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
