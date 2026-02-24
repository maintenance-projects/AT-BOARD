package kr.co.ultari.at_board.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final SsoAuthInterceptor ssoAuthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

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
}
