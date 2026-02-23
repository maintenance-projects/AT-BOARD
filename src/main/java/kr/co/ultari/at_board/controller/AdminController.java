package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.model.primary.Admin;
import kr.co.ultari.at_board.service.AdminService;
import kr.co.ultari.at_board.service.BoardCategoryService;
import kr.co.ultari.at_board.service.BoardService;
import kr.co.ultari.at_board.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final BoardService boardService;
    private final BoardCategoryService boardCategoryService;
    private final UserService userService;

    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return "pages/admin/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String adminId,
                        @RequestParam String password,
                        HttpSession session) {
        Admin admin = adminService.authenticateAdmin(adminId, password);

        if (admin == null) {
            return "redirect:/admin/login?error=true";
        }

        session.setAttribute("adminUser", admin);
        session.setAttribute("adminId", admin.getAdminId());

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("adminUser");
        session.removeAttribute("adminId");
        session.invalidate();
        return "redirect:/admin/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        // 통계 데이터
        long totalBoards = boardService.getTotalBoardCount();
        long totalUsers = userService.getTotalUserCount();
        long totalCategories = boardCategoryService.getTotalCategoryCount();
        long todayBoards = boardService.getTodayBoardCount();

        model.addAttribute("admin", admin);
        model.addAttribute("totalBoards", totalBoards);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalCategories", totalCategories);
        model.addAttribute("todayBoards", todayBoards);

        return "pages/admin/dashboard";
    }
}
