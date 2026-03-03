package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 이미지 서빙 통합 프록시
 * local mode: 로컬 디스크에서 직접 서빙
 * remote mode: 업무망 InternalFileController에 프록시
 * URL 패턴: /api/files/images/{relativePath}
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileProxyController {

    private static final String IMAGE_PREFIX = "/api/files/images/";

    private final FileStorageService fileStorageService;

    @GetMapping("/images/**")
    public ResponseEntity<Resource> serveImage(HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI();
        int idx = uri.indexOf(IMAGE_PREFIX);
        if (idx < 0) {
            return ResponseEntity.badRequest().build();
        }
        String relativePath = uri.substring(idx + IMAGE_PREFIX.length());

        if (relativePath.contains("..")) {
            log.warn("Path traversal attempt blocked: {}", relativePath);
            return ResponseEntity.badRequest().build();
        }

        Resource resource = fileStorageService.loadFile(relativePath);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = determineContentType(relativePath);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private String determineContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
