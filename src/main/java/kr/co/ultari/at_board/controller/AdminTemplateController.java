package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.model.primary.Admin;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/templates")
public class AdminTemplateController {

    @GetMapping
    public String list(Model model, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) return "redirect:/admin/login";
        model.addAttribute("admin", admin);
        return "pages/admin/template/list";
    }
}
