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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
                       @RequestParam(required = false, defaultValue = "all") String searchType,
                       @RequestParam(required = false) String keyword,
                       Model model,
                       HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        // 사용자가 볼 수 있는 게시판 목록 (전사 가나다순 > 부서 가나다순)
        List<BoardCategory> categories = boardCategoryService.getCompanyWideCategories();
        categories.sort(Comparator.comparing(BoardCategory::getName));
        List<BoardCategory> deptCategories = boardCategoryService.getDeptCategoriesForUser(currentUser.getDeptId());
        deptCategories.sort(Comparator.comparing(BoardCategory::getName));
        categories.addAll(deptCategories);

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

        // 권한 체크: 접근 가능한 게시판인지 확인
        boolean canAccess = categories.stream().anyMatch(c -> c.getId().equals(selectedCategory.getId()));
        if (!canAccess) {
            return "redirect:/board?categoryId=" + categories.get(0).getId();
        }

        // 검색 또는 일반 페이징
        Pageable pageable = PageRequest.of(page, size);
        boolean isSearching = keyword != null && !keyword.trim().isEmpty();
        Page<Board> boardsPage = isSearching
                ? boardService.searchBoards(categoryId, searchType, keyword, pageable)
                : boardService.getBoardsByCategoryIdPaged(categoryId, pageable);

        model.addAttribute("boards", boardsPage);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        return "pages/board/list";
    }

    // 모바일 무한 스크롤용 AJAX API
    @GetMapping("/api/boards")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBoardsApi(
            @RequestParam Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "all") String searchType,
            @RequestParam(required = false) String keyword,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        BoardCategory selectedCategory = boardCategoryService.getCategoryById(categoryId);
        if (selectedCategory == null) {
            return ResponseEntity.notFound().build();
        }

        // 권한 체크: 부서 계층 구조를 고려한 접근 가능 게시판 목록으로 확인
        List<BoardCategory> accessibleCategories = boardCategoryService.getCompanyWideCategories();
        accessibleCategories.addAll(boardCategoryService.getDeptCategoriesForUser(currentUser.getDeptId()));
        boolean canAccess = accessibleCategories.stream().anyMatch(c -> c.getId().equals(categoryId));
        if (!canAccess) {
            return ResponseEntity.status(403).build();
        }

        Pageable pageable = PageRequest.of(page, size);
        boolean isSearching = keyword != null && !keyword.trim().isEmpty();
        Page<Board> boardsPage = isSearching
                ? boardService.searchBoards(categoryId, searchType, keyword, pageable)
                : boardService.getBoardsByCategoryIdPaged(categoryId, pageable);

        // Board 엔티티를 직접 직렬화하면 Hibernate 프록시 문제가 발생하므로 필요한 필드만 Map으로 변환
        List<Map<String, Object>> boardDtos = boardsPage.getContent().stream().map(board -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", board.getId());
            dto.put("title", board.getTitle());
            dto.put("authorName", board.getAuthorName());
            dto.put("createdAt", board.getCreatedAt());
            dto.put("content", board.getContent());
            dto.put("likeCount", board.getLikeCount());
            dto.put("commentCount", board.getCommentCount());
            dto.put("viewCount", board.getViewCount());
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", boardDtos);
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

        // 세션 기반 조회수 중복 방지: 이 세션에서 이미 본 게시글이면 카운트 증가 안 함
        @SuppressWarnings("unchecked")
        Set<Long> viewedBoards = (Set<Long>) session.getAttribute("viewedBoards");
        if (viewedBoards == null) {
            viewedBoards = new HashSet<>();
            session.setAttribute("viewedBoards", viewedBoards);
        }

        Board board;
        if (viewedBoards.contains(id)) {
            board = boardService.getBoardByIdForUserNoCount(id, currentUser);
        } else {
            board = boardService.getBoardByIdForUser(id, currentUser);
            if (board != null) {
                viewedBoards.add(id);
            }
        }

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

        // 사용자가 글을 작성할 수 있는 카테고리 목록 (adminOnly 제외, 전사 가나다순 > 부서 가나다순)
        List<BoardCategory> categories = boardCategoryService.getCompanyWideCategories();
        categories.sort(Comparator.comparing(BoardCategory::getName));
        List<BoardCategory> deptCategories = boardCategoryService.getDeptCategoriesForUser(currentUser.getDeptId());
        deptCategories.sort(Comparator.comparing(BoardCategory::getName));
        categories.addAll(deptCategories);

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

        Board board = boardService.getBoardByIdNoCount(id);
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
