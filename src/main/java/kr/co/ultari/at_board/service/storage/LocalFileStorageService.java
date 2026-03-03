package kr.co.ultari.at_board.service.storage;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@ConditionalOnProperty(name = "file.storage.mode", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadPath, "imgs"));
            Files.createDirectories(Paths.get(uploadPath, "attach"));
            log.info("Upload directories initialized: {}", Paths.get(uploadPath).toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to initialize upload directories: {}", uploadPath, e);
        }
    }

    @Override
    public String uploadImage(MultipartFile file, String relativePath) throws IOException {
        Path dest = Paths.get(uploadPath).resolve(relativePath);
        Files.createDirectories(dest.getParent());
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        log.info("Image uploaded locally: {}", dest.toAbsolutePath());
        return "/api/files/images/" + relativePath;
    }

    @Override
    public String uploadAttachment(MultipartFile file, String relativePath) throws IOException {
        Path dest = Paths.get(uploadPath).resolve(relativePath);
        Files.createDirectories(dest.getParent());
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        log.info("Attachment uploaded locally: {}", dest.toAbsolutePath());
        return relativePath;
    }

    @Override
    public Resource loadFile(String relativePath) throws IOException {
        Path path = Paths.get(uploadPath).resolve(relativePath);
        try {
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            log.warn("File not found or not readable: {}", path.toAbsolutePath());
            return null;
        } catch (MalformedURLException e) {
            log.error("Invalid file path: {}", relativePath, e);
            return null;
        }
    }

    @Override
    public Resource loadResized(String relativePath, int maxWidth) throws IOException {
        // GIF / WebP는 리사이즈 미지원 → 원본 반환
        if (!isResizable(relativePath)) {
            return loadFile(relativePath);
        }

        String cachePath = buildCachePath(relativePath, maxWidth);
        Path cacheDisk = Paths.get(uploadPath).resolve(cachePath);

        if (!Files.exists(cacheDisk)) {
            Path origDisk = Paths.get(uploadPath).resolve(relativePath);
            if (!Files.exists(origDisk)) return null;

            BufferedImage src = ImageIO.read(origDisk.toFile());
            if (src == null) return loadFile(relativePath); // 읽기 실패 → 원본

            if (src.getWidth() <= maxWidth) {
                // 이미 충분히 작으면 원본 반환 (캐시 생성 불필요)
                return new UrlResource(origDisk.toUri());
            }

            Files.createDirectories(cacheDisk.getParent());
            Thumbnails.of(src)
                    .width(maxWidth)
                    .keepAspectRatio(true)
                    .outputQuality(0.85)
                    .toFile(cacheDisk.toFile());
            log.info("Thumbnail created: {} (maxWidth={})", cacheDisk.toAbsolutePath(), maxWidth);
        }

        try {
            Resource resource = new UrlResource(cacheDisk.toUri());
            if (resource.exists() && resource.isReadable()) return resource;
        } catch (MalformedURLException e) {
            log.error("Invalid cache path: {}", cachePath, e);
        }
        return null;
    }

    private boolean isResizable(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
    }

    private String buildCachePath(String relativePath, int maxWidth) {
        int lastDot = relativePath.lastIndexOf('.');
        if (lastDot < 0) return relativePath + "_w" + maxWidth;
        return relativePath.substring(0, lastDot) + "_w" + maxWidth + relativePath.substring(lastDot);
    }

    @Override
    public void deleteFile(String relativePath) {
        try {
            Files.deleteIfExists(Paths.get(uploadPath).resolve(relativePath));
            log.debug("File deleted: {}", relativePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", relativePath, e);
        }
    }
}
