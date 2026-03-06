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
import java.io.IOException;

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
            writeErrorPage(response, "SSO 인증 정보가 없습니다.", null);
            return false;
        }

        // 사용자 로그인 처리 (userId로 DB에서 조회)
        User user = userService.processUserLogin(userId);

        // 사용자가 DB에 없으면 접근 차단
        if (user == null) {
            log.warn("Access denied for user not in database: {}", userId);
            writeErrorPage(response, "등록되지 않은 사용자입니다.", "관리자에게 문의하세요.");
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

    private void writeErrorPage(HttpServletResponse response, String message, String subMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/html; charset=UTF-8");
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>")
          .append("<html lang=\"ko\"><head>")
          .append("<meta charset=\"UTF-8\">")
          .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
          .append("<title>접근 거부</title>")
          .append("<style>")
          .append("*{box-sizing:border-box;margin:0;padding:0}")
          .append("body{min-height:100vh;display:flex;align-items:center;justify-content:center;")
          .append("background:#f6f7fb;font-family:system-ui,-apple-system,sans-serif;padding:20px}")
          .append(".card{background:#fff;border-radius:16px;box-shadow:0 8px 24px rgba(0,0,0,.08);")
          .append("padding:52px 40px;text-align:center;max-width:420px;width:100%}")
          .append(".icon{font-size:56px;margin-bottom:20px}")
          .append(".title{font-size:20px;font-weight:700;color:#111827;margin-bottom:12px}")
          .append(".desc{font-size:14px;color:#6b7280;line-height:1.7;word-break:keep-all}")
          .append(".sub{margin-top:8px;font-size:13px;color:#9ca3af}")
          .append("@media(max-width:480px){.card{padding:40px 24px}.icon{font-size:44px}.title{font-size:18px}}")
          .append("@media(prefers-color-scheme:dark){body{background:#111827}.card{background:#1f2937;box-shadow:0 8px 24px rgba(0,0,0,.4)}.title{color:#f9fafb}.desc{color:#9ca3af}.sub{color:#6b7280}}")
          .append("</style></head><body>")
          .append("<div class=\"card\">")
          .append("<div class=\"icon\">🔒</div>")
          .append("<h1 class=\"title\">접근 거부</h1>")
          .append("<p class=\"desc\">").append(message).append("</p>");
        if (subMessage != null) {
            sb.append("<p class=\"sub\">").append(subMessage).append("</p>");
        }
        sb.append("</div></body></html>");
        response.getWriter().write(sb.toString());
    }
}
