package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.model.primary.Admin;
import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.secondary.Department;
import kr.co.ultari.at_board.repository.secondary.DepartmentRepository;
import kr.co.ultari.at_board.service.BoardCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryController {

    private final BoardCategoryService boardCategoryService;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public String list(Model model, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        List<BoardCategory> categories = boardCategoryService.getAllCategories();
        model.addAttribute("admin", admin);
        model.addAttribute("categories", categories);

        return "pages/admin/category/list";
    }

    @GetMapping("/new")
    public String createForm(Model model, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        List<Department> departments = departmentRepository.findAll();
        model.addAttribute("admin", admin);
        model.addAttribute("departments", departments);
        model.addAttribute("category", new BoardCategory());
        model.addAttribute("isEdit", false);

        return "pages/admin/category/form";
    }

    @PostMapping("/new")
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String departmentId,
                         @RequestParam(defaultValue = "true") Boolean isActive,
                         @RequestParam(defaultValue = "false") Boolean adminOnly,
                         HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        BoardCategory category = BoardCategory.builder()
                .name(name)
                .description(description)
                .deptId(departmentId != null && !departmentId.isEmpty() ? departmentId : null)
                .isActive(isActive)
                .adminOnly(adminOnly)
                .build();

        boardCategoryService.createCategory(category);

        log.info("Admin {} created category: {} (adminOnly: {})", admin.getAdminId(), name, adminOnly);

        return "redirect:/admin/categories";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        BoardCategory category = boardCategoryService.getCategoryById(id);
        if (category == null) {
            return "redirect:/admin/categories";
        }

        List<Department> departments = departmentRepository.findAll();
        model.addAttribute("admin", admin);
        model.addAttribute("departments", departments);
        model.addAttribute("category", category);
        model.addAttribute("isEdit", true);

        return "pages/admin/category/form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(defaultValue = "true") Boolean isActive,
                         @RequestParam(defaultValue = "false") Boolean adminOnly,
                         HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        BoardCategory category = boardCategoryService.getCategoryById(id);
        if (category == null) {
            return "redirect:/admin/categories";
        }

        category.setName(name);
        category.setDescription(description);
        category.setIsActive(isActive);
        category.setAdminOnly(adminOnly);

        boardCategoryService.updateCategory(id, category);

        log.info("Admin {} updated category: {} (adminOnly: {})", admin.getAdminId(), name, adminOnly);

        return "redirect:/admin/categories";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        BoardCategory category = boardCategoryService.getCategoryById(id);
        if (category != null) {
            boardCategoryService.deleteCategory(id);
            log.info("Admin {} deleted category: {}", admin.getAdminId(), category.getName());
        }

        return "redirect:/admin/categories";
    }
}
