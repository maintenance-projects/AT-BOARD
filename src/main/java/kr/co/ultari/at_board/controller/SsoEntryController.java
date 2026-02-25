package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.model.secondary.User;
import kr.co.ultari.at_board.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * /{userId} 로 접근 시 SSO 로그인 처리 후 /board 로 리다이렉트
 * URI에 userId가 노출되지 않도록 redirect 처리
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SsoEntryController {

    private final UserService userService;

    @RequestMapping(value = "/{userId}", method = {RequestMethod.GET, RequestMethod.POST})
    public String ssoEntry(@PathVariable String userId,
                           HttpSession session,
                           HttpServletResponse response) throws IOException {
        // 이미 로그인된 경우 바로 게시판으로
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/board";
        }

        User user = userService.processUserLogin(userId);

        if (user == null) {
            log.warn("SSO entry failed: user not found - {}", userId);
            writeErrorPage(response, "등록되지 않은 사용자입니다.", "관리자에게 문의하세요.");
            return null;
        }

        session.setAttribute("currentUser", user);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("deptId", user.getDeptId());

        log.info("SSO entry login: {} {} ({})",
                user.getUserName(),
                user.getPosName() != null ? user.getPosName() : "",
                userId);

        return "redirect:/board";
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
