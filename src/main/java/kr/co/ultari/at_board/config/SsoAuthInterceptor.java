package kr.co.ultari.at_board.config;

import kr.co.ultari.at_board.model.secondary.User;
import kr.co.ultari.at_board.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
@RequiredArgsConstructor
@Slf4j
public class SsoAuthInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();

        // 이미 로그인된 사용자가 있으면 통과
        if (session.getAttribute("currentUser") != null) {
            return true;
        }

        // SSO 헤더에서 사용자 ID 추출
        String userId = request.getHeader("X-User-Id");

        // 헤더가 없으면 접근 차단
        if (userId == null || userId.isEmpty()) {
            log.warn("Access denied: X-User-Id header is missing");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/html; charset=UTF-8");
            response.getWriter().write(
                "<!DOCTYPE html>" +
                "<html><head><meta charset=\"UTF-8\"><title>접근 거부</title>" +
                "<style>body{font-family:sans-serif;text-align:center;padding:50px;}" +
                "h1{color:#d32f2f;}p{color:#666;}</style></head><body>" +
                "<h1>접근 거부</h1>" +
                "<p>SSO 인증 정보가 없습니다.</p>" +
                "</body></html>"
            );
            return false;
        }

        // 사용자 로그인 처리 (userId로 DB에서 조회)
        User user = userService.processUserLogin(userId);

        // 사용자가 DB에 없으면 접근 차단
        if (user == null) {
            log.warn("Access denied for user not in database: {}", userId);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/html; charset=UTF-8");
            response.getWriter().write(
                "<!DOCTYPE html>" +
                "<html><head><meta charset=\"UTF-8\"><title>접근 거부</title>" +
                "<style>body{font-family:sans-serif;text-align:center;padding:50px;}" +
                "h1{color:#d32f2f;}p{color:#666;}</style></head><body>" +
                "<h1>접근 거부</h1>" +
                "<p>등록되지 않은 사용자입니다.</p>" +
                "<p>사용자 ID: <strong>" + userId + "</strong></p>" +
                "<p>관리자에게 문의하세요.</p>" +
                "</body></html>"
            );
            return false;
        }

        // 세션에 사용자 정보 저장
        session.setAttribute("currentUser", user);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("deptId", user.getDeptId());

        log.info("SSO authentication successful for user: {} {} ({})",
                user.getUserName(), user.getPosName() != null ? user.getPosName() : "", user.getUserId());

        return true;
    }
}
