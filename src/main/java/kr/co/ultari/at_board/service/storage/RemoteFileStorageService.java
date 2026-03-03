package kr.co.ultari.at_board.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@ConditionalOnProperty(name = "file.storage.mode", havingValue = "remote")
@Slf4j
public class RemoteFileStorageService implements FileStorageService {

    @Value("${file.storage.remote.base-url}")
    private String remoteBaseUrl;

    @Value("${file.storage.remote.internal-secret:}")
    private String internalSecret;

    @Autowired
    @Qualifier("fileRestTemplate")
    private RestTemplate fileRestTemplate;

    @Override
    public String uploadImage(MultipartFile file, String relativePath) throws IOException {
        String url = remoteBaseUrl + "/internal/files/upload?path=" + relativePath;
        ResponseEntity<Void> response = postFile(url, file);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Remote image upload failed: status=" + response.getStatusCode());
        }
        log.info("Image uploaded remotely: {}", relativePath);
        return "/api/files/images/" + relativePath;
    }

    @Override
    public String uploadAttachment(MultipartFile file, String relativePath) throws IOException {
        String url = remoteBaseUrl + "/internal/files/upload?path=" + relativePath;
        ResponseEntity<Void> response = postFile(url, file);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Remote attachment upload failed: status=" + response.getStatusCode());
        }
        log.info("Attachment uploaded remotely: {}", relativePath);
        return relativePath;
    }

    @Override
    public Resource loadFile(String relativePath) throws IOException {
        String url = remoteBaseUrl + "/internal/files/serve?path=" + relativePath;
        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response =
                    fileRestTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return new ByteArrayResource(response.getBody());
            }
            log.warn("Remote file not found: {}", relativePath);
            return null;
        } catch (Exception e) {
            log.error("Failed to load remote file: {}", relativePath, e);
            return null;
        }
    }

    @Override
    public void deleteFile(String relativePath) {
        String url = remoteBaseUrl + "/internal/files/delete?path=" + relativePath;
        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            fileRestTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.debug("Remote file deleted: {}", relativePath);
        } catch (Exception e) {
            log.error("Failed to delete remote file: {}", relativePath, e);
        }
    }

    private ResponseEntity<Void> postFile(String url, MultipartFile file) throws IOException {
        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        final byte[] bytes = file.getBytes();
        final String originalFilename = file.getOriginalFilename();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return originalFilename;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        return fileRestTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (internalSecret != null && !internalSecret.trim().isEmpty()) {
            headers.set("X-Internal-Secret", internalSecret);
        }
        return headers;
    }
}
