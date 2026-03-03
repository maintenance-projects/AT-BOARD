package kr.co.ultari.at_board.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
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
    public void deleteFile(String relativePath) {
        try {
            Files.deleteIfExists(Paths.get(uploadPath).resolve(relativePath));
            log.debug("File deleted: {}", relativePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", relativePath, e);
        }
    }
}
