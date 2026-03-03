package kr.co.ultari.at_board.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty(name = "file.storage.mode", havingValue = "remote")
@Slf4j
public class FileStorageConfig {

    @Value("${file.storage.remote.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${file.storage.remote.read-timeout-ms:60000}")
    private int readTimeoutMs;

    @Bean("fileRestTemplate")
    public RestTemplate fileRestTemplate() {
        try {
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLContext(SSLContextBuilder.create()
                            .loadTrustMaterial(null, (cert, authType) -> true)
                            .build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
            HttpComponentsClientHttpRequestFactory factory =
                    new HttpComponentsClientHttpRequestFactory(httpClient);
            factory.setConnectTimeout(connectTimeoutMs);
            factory.setReadTimeout(readTimeoutMs);
            log.info("fileRestTemplate created: connectTimeout={}ms, readTimeout={}ms",
                    connectTimeoutMs, readTimeoutMs);
            return new RestTemplate(factory);
        } catch (Exception e) {
            log.warn("SSL 설정 실패, 기본 fileRestTemplate 사용: {}", e.getMessage());
            return new RestTemplate();
        }
    }
}
