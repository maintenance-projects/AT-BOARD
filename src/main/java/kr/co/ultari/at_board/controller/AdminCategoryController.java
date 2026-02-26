package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.model.primary.Admin;
import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.secondary.Dept;
import kr.co.ultari.at_board.repository.secondary.DepartmentRepository;
import kr.co.ultari.at_board.service.BoardCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryController {

    private final BoardCategoryService boardCategoryService;
    private final DepartmentRepository departmentRepository;

    private static final int PAGE_SIZE = 15;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String keyword,
                       Model model,
                       HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        PageRequest pageable = PageRequest.of(page, PAGE_SIZE);
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        Page<BoardCategory> categories = hasKeyword
                ? boardCategoryService.searchCategoriesByName(keyword, pageable)
                : boardCategoryService.getAllCategoriesPaged(pageable);

        model.addAttribute("admin", admin);
        model.addAttribute("categories", categories);
        model.addAttribute("keyword", keyword);

        return "pages/admin/category/list";
    }

    @GetMapping("/new")
    public String createForm(Model model, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        List<Dept> departments = getSortedDepartments();
        model.addAttribute("admin", admin);
        model.addAttribute("departments", departments);
        model.addAttribute("category", new BoardCategory());
        model.addAttribute("selectedDeptIds", new java.util.HashSet<String>());
        model.addAttribute("isEdit", false);

        return "pages/admin/category/form";
    }

    @PostMapping("/new")
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) List<String> deptIds,
                         @RequestParam(defaultValue = "false") Boolean isActive,
                         @RequestParam(defaultValue = "false") Boolean adminOnly,
                         HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        java.util.Set<String> deptIdSet = deptIds != null ? new java.util.HashSet<>(deptIds) : new java.util.HashSet<>();
        BoardCategory category = BoardCategory.builder()
                .name(name)
                .description(description)
                .deptIds(deptIdSet)
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

        List<Dept> departments = getSortedDepartments();
        model.addAttribute("admin", admin);
        model.addAttribute("departments", departments);
        model.addAttribute("category", category);
        model.addAttribute("selectedDeptIds", category.getDeptIds());
        model.addAttribute("isEdit", true);

        return "pages/admin/category/form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(defaultValue = "false") Boolean isActive,
                         @RequestParam(defaultValue = "false") Boolean adminOnly,
                         @RequestParam(required = false) List<String> deptIds,
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
        category.setDeptIds(deptIds != null ? new java.util.HashSet<>(deptIds) : new java.util.HashSet<>());

        boardCategoryService.updateCategory(id, category);

        log.info("Admin {} updated category: {} (adminOnly: {})", admin.getAdminId(), name, adminOnly);

        return "redirect:/admin/categories";
    }

    private List<Dept> getSortedDepartments() {
        List<Dept> departments = departmentRepository.findAll();
        departments.sort((a, b) -> {
            String oa = (a.getDeptOrder() != null && !a.getDeptOrder().isEmpty()) ? a.getDeptOrder() : "999999";
            String ob = (b.getDeptOrder() != null && !b.getDeptOrder().isEmpty()) ? b.getDeptOrder() : "999999";
            return oa.compareTo(ob);
        });
        return departments;
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
