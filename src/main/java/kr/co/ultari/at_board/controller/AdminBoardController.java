package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.model.primary.Admin;
import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.BoardAttachment;
import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.primary.Comment;
import kr.co.ultari.at_board.service.BoardAttachmentService;
import kr.co.ultari.at_board.service.BoardCategoryService;
import kr.co.ultari.at_board.service.BoardService;
import kr.co.ultari.at_board.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Comment import already added via CommentService

@Controller
@RequestMapping("/admin/boards")
@RequiredArgsConstructor
@Slf4j
public class AdminBoardController {

    private final BoardService boardService;
    private final BoardCategoryService boardCategoryService;
    private final BoardAttachmentService boardAttachmentService;
    private final CommentService commentService;

    private static final int PAGE_SIZE = 20;

    @GetMapping
    public String list(@RequestParam(required = false) Long categoryId,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false, defaultValue = "all") String searchType,
                       @RequestParam(required = false) String keyword,
                       Model model,
                       HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        PageRequest pageable = PageRequest.of(page, PAGE_SIZE);
        Page<Board> boards = boardService.adminSearchBoards(categoryId, searchType, keyword, pageable);

        List<BoardCategory> categories = boardCategoryService.getAllCategories();

        List<Long> boardIds = boards.getContent().stream().map(Board::getId).collect(Collectors.toList());
        Set<Long> attachedBoardIds = boardAttachmentService.getBoardIdsWithAttachments(boardIds);

        model.addAttribute("admin", admin);
        model.addAttribute("boards", boards);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("attachedBoardIds", attachedBoardIds);

        return "pages/admin/board/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(required = false) Long categoryId,
                         @RequestParam(required = false) String searchType,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(defaultValue = "0") int commentPage,
                         Model model, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        Board board = boardService.getBoardByIdNoCount(id);
        if (board == null) {
            return "redirect:/admin/boards";
        }

        List<BoardAttachment> attachments = boardAttachmentService.getByBoardId(id);
        Page<Comment> commentPageData = commentService.getCommentsByBoardPaged(board, commentPage, "asc");

        model.addAttribute("admin", admin);
        model.addAttribute("board", board);
        model.addAttribute("attachments", attachments);
        model.addAttribute("comments", commentPageData.getContent());
        model.addAttribute("commentPage", commentPageData.getNumber());
        model.addAttribute("commentTotalPages", commentPageData.getTotalPages());
        model.addAttribute("backCategoryId", categoryId);
        model.addAttribute("backSearchType", searchType);
        model.addAttribute("backKeyword", keyword);

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
                        @RequestParam(required = false) List<Long> attachmentIds,
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

        Board savedBoard = boardService.createBoardByAdmin(board);

        if (attachmentIds != null && !attachmentIds.isEmpty()) {
            boardAttachmentService.assignToBoard(attachmentIds, savedBoard.getId());
        }

        log.info("Admin {} created board: {}", admin.getAdminId(), title);

        return "redirect:/admin/boards?categoryId=" + categoryId;
    }

    @PostMapping("/{boardId}/comments")
    public String createComment(@PathVariable Long boardId,
                                @RequestParam String content,
                                @RequestParam(required = false) Long categoryId,
                                @RequestParam(required = false) String searchType,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(defaultValue = "0") int commentPage,
                                HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) return "redirect:/admin/login";

        if (content == null || content.trim().isEmpty()) {
            return buildDetailRedirect(boardId, categoryId, searchType, keyword, commentPage);
        }

        Board board = boardService.getBoardByIdNoCount(boardId);
        if (board != null) {
            commentService.createCommentByAdmin(board, content.trim(), admin);
        }
        return buildDetailRedirect(boardId, categoryId, searchType, keyword, commentPage);
    }

    @PostMapping("/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long commentId,
                                @RequestParam Long boardId,
                                @RequestParam(required = false) Long categoryId,
                                @RequestParam(required = false) String searchType,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(defaultValue = "0") int commentPage,
                                HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) return "redirect:/admin/login";

        commentService.deleteCommentByAdmin(commentId);
        return buildDetailRedirect(boardId, categoryId, searchType, keyword, commentPage);
    }

    private String buildDetailRedirect(Long boardId, Long categoryId, String searchType, String keyword, int commentPage) {
        StringBuilder url = new StringBuilder("redirect:/admin/boards/").append(boardId);
        StringBuilder params = new StringBuilder();
        if (categoryId != null) params.append("categoryId=").append(categoryId);
        if (searchType != null && !searchType.trim().isEmpty()) {
            if (params.length() > 0) params.append("&");
            params.append("searchType=").append(searchType);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            if (params.length() > 0) params.append("&");
            params.append("keyword=").append(keyword);
        }
        if (commentPage > 0) {
            if (params.length() > 0) params.append("&");
            params.append("commentPage=").append(commentPage);
        }
        if (params.length() > 0) url.append("?").append(params);
        return url.toString();
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) Long categoryId,
                         HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        Board board = boardService.getBoardByIdNoCount(id);
        if (board != null) {
            boardAttachmentService.deleteByBoardId(id);
            boardService.deleteBoardByAdmin(id);
            log.info("Admin {} deleted board: {}", admin.getAdminId(), board.getTitle());
        }

        if (categoryId != null) {
            return "redirect:/admin/boards?categoryId=" + categoryId;
        }
        return "redirect:/admin/boards";
    }
}
