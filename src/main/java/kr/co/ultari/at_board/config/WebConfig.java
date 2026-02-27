package kr.co.ultari.at_board.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableAsync
@RequiredArgsConstructor
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    private final SsoAuthInterceptor ssoAuthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    @Value("${notification.msg.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${notification.msg.read-timeout-ms:10000}")
    private int readTimeoutMs;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // SSO 인증 인터셉터 (사용자 페이지에 적용)
        registry.addInterceptor(ssoAuthInterceptor)
                .addPathPatterns("/board/**", "/menu", "/notice")
                .excludePathPatterns("/admin/**");

        // 관리자 인증 인터셉터 (관리자 페이지에만 적용)
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");
    }

    @Bean
    public RestTemplate restTemplate() {
        try {
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLContext(SSLContextBuilder.create()
                            .loadTrustMaterial(null, (cert, authType) -> true)
                            .build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            factory.setConnectTimeout(connectTimeoutMs);
            factory.setReadTimeout(readTimeoutMs);
            return new RestTemplate(factory);
        } catch (Exception e) {
            log.warn("SSL 설정 실패, 기본 RestTemplate 사용: {}", e.getMessage());
            return new RestTemplate();
        }
    }
}
