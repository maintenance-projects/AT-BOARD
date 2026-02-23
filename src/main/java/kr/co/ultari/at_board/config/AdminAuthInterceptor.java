package kr.co.ultari.at_board.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
@Slf4j
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();

        // 관리자 로그인 페이지는 체크하지 않음
        String requestURI = request.getRequestURI();
        if (requestURI.equals("/admin/login") || requestURI.startsWith("/admin/login/")) {
            return true;
        }

        // 세션에 관리자 정보가 있는지 확인
        if (session.getAttribute("adminUser") == null) {
            log.warn("Unauthorized admin access attempt: {}", requestURI);
            response.sendRedirect("/admin/login");
            return false;
        }

        return true;
    }
}
