package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 업무망 전용 내부 파일 API (local mode에서만 활성화)
 * 인터넷망 게시판이 업무망 게시판에 파일 저장/조회/삭제 요청 시 사용
 */
@RestController
@RequestMapping("/internal/files")
@ConditionalOnProperty(name = "file.storage.mode", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class InternalFileController {

    private final FileStorageService fileStorageService;

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    @Value("${file.storage.remote.internal-secret:}")
    private String internalSecret;

    /**
     * 파일 저장
     * @param path 저장할 상대 경로 (예: imgs/2024/01/01/uuid.jpg 또는 attach/2024/01/01/uuid.pdf)
     */
    @PostMapping("/upload")
    public ResponseEntity<Void> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("path") String path,
            HttpServletRequest request) throws IOException {

        if (!checkSecret(request)) {
            return ResponseEntity.status(403).build();
        }
        if (path.contains("..")) {
            log.warn("Path traversal attempt blocked: {}", path);
            return ResponseEntity.badRequest().build();
        }

        Path dest = Paths.get(uploadPath).resolve(path);
        Files.createDirectories(dest.getParent());
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        log.info("Internal file stored: {}", dest.toAbsolutePath());
        return ResponseEntity.ok().build();
    }

    /**
     * 파일 서빙
     * @param path 상대 경로 (예: imgs/2024/01/01/uuid.jpg)
     * @param width 최대 너비 (선택, 있으면 리사이즈 캐시 반환)
     */
    @GetMapping("/serve")
    public ResponseEntity<Resource> serveFile(
            @RequestParam("path") String path,
            @RequestParam(value = "w", required = false) Integer width,
            HttpServletRequest request) throws IOException {

        if (!checkSecret(request)) {
            return ResponseEntity.status(403).build();
        }
        if (path.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = (width != null)
                ? fileStorageService.loadResized(path, width)
                : fileStorageService.loadFile(path);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(determineContentType(path)))
                .body(resource);
    }

    /**
     * 파일 삭제
     * @param path 상대 경로
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteFile(
            @RequestParam("path") String path,
            HttpServletRequest request) {

        if (!checkSecret(request)) {
            return ResponseEntity.status(403).build();
        }
        if (path.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        fileStorageService.deleteFile(path);
        log.info("Internal file deleted: {}", path);
        return ResponseEntity.ok().build();
    }

    private boolean checkSecret(HttpServletRequest request) {
        if (internalSecret == null || internalSecret.trim().isEmpty()) {
            return true; // 시크릿 미설정 시 모두 허용 (방화벽으로 접근 제어 가정)
        }
        return internalSecret.equals(request.getHeader("X-Internal-Secret"));
    }

    private String determineContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
}
