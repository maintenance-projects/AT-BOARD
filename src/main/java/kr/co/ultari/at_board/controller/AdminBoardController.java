package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.model.primary.Admin;
import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.service.BoardCategoryService;
import kr.co.ultari.at_board.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/boards")
@RequiredArgsConstructor
@Slf4j
public class AdminBoardController {

    private final BoardService boardService;
    private final BoardCategoryService boardCategoryService;

    @GetMapping
    public String list(@RequestParam(required = false) Long categoryId,
                       Model model,
                       HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        List<Board> boards;
        if (categoryId != null) {
            BoardCategory category = boardCategoryService.getCategoryById(categoryId);
            if (category != null) {
                boards = boardService.getAllBoards().stream()
                        .filter(b -> b.getCategory().getId().equals(categoryId))
                        .collect(Collectors.toList());
                model.addAttribute("selectedCategory", category);
            } else {
                boards = boardService.getAllBoards();
            }
        } else {
            boards = boardService.getAllBoards();
        }

        List<BoardCategory> categories = boardCategoryService.getAllCategories();

        model.addAttribute("admin", admin);
        model.addAttribute("boards", boards);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryId", categoryId);

        return "pages/admin/board/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        Board board = boardService.getBoardById(id);
        if (board == null) {
            return "redirect:/admin/boards";
        }

        model.addAttribute("admin", admin);
        model.addAttribute("board", board);

        return "pages/admin/board/detail";
    }

    @GetMapping("/write")
    public String writeForm(@RequestParam(required = false) Long categoryId,
                            Model model,
                            HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        List<BoardCategory> categories = boardCategoryService.getAllCategories();

        model.addAttribute("admin", admin);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategoryId", categoryId);

        return "pages/admin/board/write";
    }

    @PostMapping("/write")
    public String write(@RequestParam String title,
                        @RequestParam String content,
                        @RequestParam Long categoryId,
                        HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        BoardCategory category = boardCategoryService.getCategoryById(categoryId);
        if (category == null) {
            return "redirect:/admin/boards/write";
        }

        // 관리자 이름으로 게시물 작성
        Board board = Board.builder()
                .title(title)
                .content(content)
                .userId("admin_" + admin.getAdminId())
                .authorName(admin.getAdminName())
                .authorPosName("관리자")
                .category(category)
                .viewCount(0)
                .build();

        boardService.createBoardByAdmin(board);

        log.info("Admin {} created board: {}", admin.getAdminId(), title);

        return "redirect:/admin/boards?categoryId=" + categoryId;
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        Board board = boardService.getBoardById(id);
        if (board != null) {
            boardService.deleteBoardByAdmin(id);
            log.info("Admin {} deleted board: {}", admin.getAdminId(), board.getTitle());
        }

        return "redirect:/admin/boards";
    }
}
