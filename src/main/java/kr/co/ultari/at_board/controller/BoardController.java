package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.primary.Comment;
import kr.co.ultari.at_board.model.secondary.User;
import kr.co.ultari.at_board.service.BoardCategoryService;
import kr.co.ultari.at_board.service.BoardLikeService;
import kr.co.ultari.at_board.service.BoardService;
import kr.co.ultari.at_board.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
@Slf4j
public class BoardController {

    private final BoardService boardService;
    private final BoardCategoryService boardCategoryService;
    private final CommentService commentService;
    private final BoardLikeService boardLikeService;

    @GetMapping
    public String list(@RequestParam(required = false) Long categoryId,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model,
                       HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        // 사용자가 볼 수 있는 게시판 목록
        List<BoardCategory> categories = boardCategoryService.getCompanyWideCategories();
        BoardCategory deptCategory = boardCategoryService.getCategoryByDeptId(currentUser.getDeptId());
        if (deptCategory != null) {
            categories.add(0, deptCategory);
        }

        // 게시판이 없으면 에러
        if (categories.isEmpty()) {
            model.addAttribute("boards", Page.empty());
            model.addAttribute("categories", categories);
            model.addAttribute("currentUser", currentUser);
            return "pages/board/list";
        }

        // categoryId가 없으면 첫 번째 게시판으로 리다이렉트
        if (categoryId == null) {
            return "redirect:/board?categoryId=" + categories.get(0).getId();
        }

        // 선택된 게시판 조회
        BoardCategory selectedCategory = boardCategoryService.getCategoryById(categoryId);
        if (selectedCategory == null) {
            return "redirect:/board?categoryId=" + categories.get(0).getId();
        }

        // 권한 체크: 부서 게시판이면 같은 부서만 조회 가능
        if (selectedCategory.getDeptId() != null &&
                !selectedCategory.getDeptId().equals(currentUser.getDeptId())) {
            return "redirect:/board?categoryId=" + categories.get(0).getId();
        }

        // 페이징 처리
        Pageable pageable = PageRequest.of(page, size);
        Page<Board> boardsPage = boardService.getBoardsByCategoryIdPaged(categoryId, pageable);

        model.addAttribute("boards", boardsPage);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("currentUser", currentUser);
        return "pages/board/list";
    }

    // 모바일 무한 스크롤용 AJAX API
    @GetMapping("/api/boards")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBoardsApi(
            @RequestParam Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        BoardCategory selectedCategory = boardCategoryService.getCategoryById(categoryId);
        if (selectedCategory == null) {
            return ResponseEntity.notFound().build();
        }

        // 권한 체크
        if (selectedCategory.getDeptId() != null &&
                !selectedCategory.getDeptId().equals(currentUser.getDeptId())) {
            return ResponseEntity.status(403).build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Board> boardsPage = boardService.getBoardsByCategoryIdPaged(categoryId, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", boardsPage.getContent());
        response.put("totalPages", boardsPage.getTotalPages());
        response.put("totalElements", boardsPage.getTotalElements());
        response.put("currentPage", boardsPage.getNumber());
        response.put("hasNext", boardsPage.hasNext());
        response.put("hasPrevious", boardsPage.hasPrevious());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        Board board = boardService.getBoardByIdForUser(id, currentUser);
        if (board == null) {
            return "redirect:/board";
        }

        // 댓글 목록 조회
        List<Comment> comments = commentService.getCommentsByBoard(board);

        // 좋아요 여부 확인
        boolean isLiked = boardLikeService.isLiked(id, currentUser.getUserId());

        model.addAttribute("board", board);
        model.addAttribute("comments", comments);
        model.addAttribute("isLiked", isLiked);
        model.addAttribute("currentUser", currentUser);
        return "pages/board/detail";
    }

    @GetMapping("/write")
    public String writeForm(@RequestParam(required = false) Long categoryId,
                            Model model,
                            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        // 사용자가 글을 작성할 수 있는 카테고리 목록 (adminOnly 제외)
        List<BoardCategory> categories = boardCategoryService.getCompanyWideCategories();
        BoardCategory deptCategory = boardCategoryService.getCategoryByDeptId(currentUser.getDeptId());
        if (deptCategory != null) {
            categories.add(0, deptCategory);
        }

        // adminOnly 게시판 제외
        categories.removeIf(cat -> cat.getAdminOnly() != null && cat.getAdminOnly());

        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("currentUser", currentUser);
        return "pages/board/write";
    }

    @PostMapping("/write")
    public String write(@RequestParam String title,
                        @RequestParam String content,
                        @RequestParam Long categoryId,
                        HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        BoardCategory category = boardCategoryService.getCategoryById(categoryId);
        if (category == null) {
            return "redirect:/board/write";
        }

        // adminOnly 게시판에는 일반 사용자가 작성 불가
        if (category.getAdminOnly() != null && category.getAdminOnly()) {
            log.warn("User {} tried to write to admin-only category: {}", currentUser.getUserId(), categoryId);
            return "redirect:/board?categoryId=" + categoryId;
        }

        try {
            boardService.createBoard(title, content, currentUser, category);
        } catch (IllegalArgumentException e) {
            log.error("Failed to create board: {}", e.getMessage());
            return "redirect:/board/write";
        }

        return "redirect:/board?categoryId=" + categoryId;
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        Board board = boardService.getBoardByIdForUser(id, currentUser);
        if (board == null || !board.getUserId().equals(currentUser.getUserId())) {
            return "redirect:/board";
        }

        model.addAttribute("board", board);
        model.addAttribute("currentUser", currentUser);
        return "pages/board/edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       @RequestParam String title,
                       @RequestParam String content,
                       HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        try {
            boardService.updateBoard(id, title, content, currentUser);
        } catch (IllegalArgumentException e) {
            log.error("Failed to update board: {}", e.getMessage());
            return "redirect:/board/" + id;
        }

        return "redirect:/board/" + id;
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) Long categoryId,
                         HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        Long boardCategoryId = categoryId;
        if (boardCategoryId == null) {
            Board board = boardService.getBoardByIdForUser(id, currentUser);
            if (board != null && board.getCategory() != null) {
                boardCategoryId = board.getCategory().getId();
            }
        }

        try {
            boardService.deleteBoard(id, currentUser);
        } catch (IllegalArgumentException e) {
            log.error("Failed to delete board: {}", e.getMessage());
        }

        if (boardCategoryId != null) {
            return "redirect:/board?categoryId=" + boardCategoryId;
        }
        return "redirect:/board";
    }

    // 댓글 작성
    @PostMapping("/{boardId}/comments")
    public String createComment(@PathVariable Long boardId,
                                 @RequestParam String content,
                                 HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        Board board = boardService.getBoardById(boardId);
        if (board == null) {
            return "redirect:/board";
        }

        try {
            commentService.createComment(board, content, currentUser);
        } catch (Exception e) {
            log.error("Failed to create comment: {}", e.getMessage());
        }

        return "redirect:/board/" + boardId;
    }

    // 댓글 삭제
    @PostMapping("/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long commentId,
                                 @RequestParam Long boardId,
                                 HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        try {
            commentService.deleteComment(commentId, currentUser);
        } catch (Exception e) {
            log.error("Failed to delete comment: {}", e.getMessage());
        }

        return "redirect:/board/" + boardId;
    }

    // 좋아요 토글 (AJAX)
    @PostMapping("/{boardId}/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long boardId,
                                                           HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        boolean isLiked = boardLikeService.toggleLike(boardId, currentUser.getUserId());
        Board board = boardService.getBoardById(boardId);

        Map<String, Object> response = new HashMap<>();
        response.put("isLiked", isLiked);
        response.put("likeCount", board != null ? board.getLikeCount() : 0);

        return ResponseEntity.ok(response);
    }
}
