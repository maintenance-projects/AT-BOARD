package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.BoardAttachment;
import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.primary.BoardTemplate;
import kr.co.ultari.at_board.model.primary.Comment;
import kr.co.ultari.at_board.model.secondary.User;
import kr.co.ultari.at_board.service.AppSettingService;
import kr.co.ultari.at_board.service.BoardAttachmentService;
import kr.co.ultari.at_board.service.BoardCategoryService;
import kr.co.ultari.at_board.service.BoardLikeService;
import kr.co.ultari.at_board.service.BoardService;
import kr.co.ultari.at_board.service.BoardTemplateService;
import kr.co.ultari.at_board.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
    private final BoardAttachmentService boardAttachmentService;
    private final AppSettingService appSettingService;
    private final BoardTemplateService boardTemplateService;

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

        // 사용자가 볼 수 있는 게시판 목록: [전사](이름순) > [부서](deptOrder순) > 없음(이름순)
        List<BoardCategory> categories = buildSortedCategories(currentUser);

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

        Map<String, List<Long>> newPostsByCategory = boardService.getNewPostsByCategoryMap(LocalDateTime.now().minusHours(24));

        List<Long> boardIds = boardsPage.getContent().stream().map(Board::getId).collect(Collectors.toList());
        Set<Long> likedBoardIds = boardLikeService.getLikedBoardIds(boardIds, currentUser.getUserId());
        Set<Long> commentedBoardIds = commentService.getCommentedBoardIds(boardIds, currentUser.getUserId());
        Set<Long> attachedBoardIds = boardAttachmentService.getBoardIdsWithAttachments(boardIds);

        model.addAttribute("boards", boardsPage);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("newPostsByCategory", newPostsByCategory);
        model.addAttribute("likedBoardIds", likedBoardIds);
        model.addAttribute("commentedBoardIds", commentedBoardIds);
        model.addAttribute("attachedBoardIds", attachedBoardIds);
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
        List<Long> apiPageBoardIds = boardsPage.getContent().stream().map(Board::getId).collect(Collectors.toList());
        Set<Long> apiLikedIds = boardLikeService.getLikedBoardIds(apiPageBoardIds, currentUser.getUserId());
        Set<Long> apiCommentedIds = commentService.getCommentedBoardIds(apiPageBoardIds, currentUser.getUserId());
        Set<Long> apiAttachedIds = boardAttachmentService.getBoardIdsWithAttachments(apiPageBoardIds);

        List<Map<String, Object>> boardDtos = boardsPage.getContent().stream().map(board -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", board.getId());
            dto.put("title", board.getTitle());
            dto.put("userId", board.getUserId());
            dto.put("authorName", board.getAuthorName());
            dto.put("authorNameOnly", board.getAuthorNameOnly());
            dto.put("createdAt", board.getCreatedAt());
            dto.put("createdAtEpochMilli", board.getCreatedAtEpochMilli());
            dto.put("categoryId", board.getCategory() != null ? board.getCategory().getId() : null);
            dto.put("content", board.getContent());
            dto.put("likeCount", board.getLikeCount());
            dto.put("commentCount", board.getCommentCount());
            dto.put("viewCount", board.getViewCount());
            dto.put("isLiked", apiLikedIds.contains(board.getId()));
            dto.put("isCommented", apiCommentedIds.contains(board.getId()));
            dto.put("hasAttachment", apiAttachedIds.contains(board.getId()));
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

    @RequestMapping(value = "/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String detail(@PathVariable Long id,
                         @RequestParam(required = false) Long categoryId,
                         @RequestParam(required = false) String searchType,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(defaultValue = "0") int commentPage,
                         @RequestParam(defaultValue = "asc") String commentSort,
                         Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        // 세션 기반 조회수 중복 방지: 이 세션에서 이미 본 게시글이면 카운트 증가 안 함
        @SuppressWarnings("unchecked")
        Set<Long> viewedBoards = (Set<Long>) session.getAttribute("viewedBoards");
        if (viewedBoards == null) {
            viewedBoards = Collections.synchronizedSet(new HashSet<>());
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

        // 댓글 목록 조회 (페이징)
        Page<Comment> commentPage2 = commentService.getCommentsByBoardPaged(board, commentPage, commentSort);

        // 좋아요 여부 확인
        boolean isLiked = boardLikeService.isLiked(id, currentUser.getUserId());

        // 첨부파일 목록
        List<BoardAttachment> attachments = boardAttachmentService.getByBoardId(id);

        Long backCategoryId = categoryId != null ? categoryId : (board.getCategory() != null ? board.getCategory().getId() : null);

        model.addAttribute("board", board);
        model.addAttribute("comments", commentPage2.getContent());
        model.addAttribute("commentPage", commentPage2.getNumber());
        model.addAttribute("commentTotalPages", commentPage2.getTotalPages());
        model.addAttribute("commentPageRange", buildCommentPageRange(commentPage2.getNumber(), commentPage2.getTotalPages()));
        model.addAttribute("commentSort", commentSort);
        model.addAttribute("isLiked", isLiked);
        model.addAttribute("attachments", attachments);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("backCategoryId", backCategoryId);
        model.addAttribute("backSearchType", searchType);
        model.addAttribute("backKeyword", keyword);
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

        // 사용자가 글을 작성할 수 있는 카테고리 목록 (adminOnly 제외): [전사](이름순) > [부서](deptOrder순) > 없음(이름순)
        List<BoardCategory> categories = buildSortedCategories(currentUser);

        // adminOnly 게시판 제외
        categories.removeIf(cat -> cat.getAdminOnly() != null && cat.getAdminOnly());

        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("maxAttachmentSizeMb", appSettingService.getMaxAttachmentSize() / (1024 * 1024));
        model.addAttribute("blockedExtensionsStr", String.join(",", appSettingService.getBlockedExtensions()));
        model.addAttribute("retentionRules", appSettingService.getRetentionRules());
        List<BoardTemplate> activeTemplates = boardTemplateService.getActiveTemplates();
        model.addAttribute("templates", activeTemplates);
        model.addAttribute("templateGroups", boardTemplateService.getActiveTemplatesGrouped());
        return "pages/board/write";
    }

    @PostMapping("/write")
    public String write(@RequestParam String title,
                        @RequestParam String content,
                        @RequestParam Long categoryId,
                        @RequestParam(required = false) List<Long> attachmentIds,
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
            Board board = boardService.createBoard(title, content, currentUser, category);
            if (attachmentIds != null && !attachmentIds.isEmpty()) {
                boardAttachmentService.assignToBoard(attachmentIds, board.getId());
            }
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

        List<BoardAttachment> attachments = boardAttachmentService.getByBoardId(id);

        model.addAttribute("board", board);
        model.addAttribute("attachments", attachments);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("maxAttachmentSizeMb", appSettingService.getMaxAttachmentSize() / (1024 * 1024));
        model.addAttribute("blockedExtensionsStr", String.join(",", appSettingService.getBlockedExtensions()));
        model.addAttribute("retentionRules", appSettingService.getRetentionRules());
        return "pages/board/edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       @RequestParam String title,
                       @RequestParam String content,
                       @RequestParam(required = false) List<Long> attachmentIds,
                       @RequestParam(required = false) List<Long> removedAttachmentIds,
                       HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        try {
            boardService.updateBoard(id, title, content, currentUser);

            // 삭제 요청된 첨부파일 제거
            if (removedAttachmentIds != null) {
                for (Long attachmentId : removedAttachmentIds) {
                    boardAttachmentService.deleteAttachment(attachmentId);
                }
            }

            // 새 첨부파일 할당
            if (attachmentIds != null && !attachmentIds.isEmpty()) {
                boardAttachmentService.assignToBoard(attachmentIds, id);
            }
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
            boardAttachmentService.deleteByBoardId(id);
            boardService.deleteBoard(id, currentUser);
        } catch (IllegalArgumentException e) {
            log.error("Failed to delete board: {}", e.getMessage());
        }

        if (boardCategoryId != null) {
            return "redirect:/board?categoryId=" + boardCategoryId;
        }
        return "redirect:/board";
    }

    // 댓글 섹션 프래그먼트 (AJAX 새로고침용)
    @GetMapping("/{boardId}/comments/fragment")
    public String commentsFragment(@PathVariable Long boardId,
                                   @RequestParam(defaultValue = "0") int commentPage,
                                   @RequestParam(defaultValue = "asc") String commentSort,
                                   Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        Board board = boardService.getBoardByIdNoCount(boardId);
        if (board == null) {
            return "redirect:/board";
        }

        Page<Comment> commentPageData = commentService.getCommentsByBoardPaged(board, commentPage, commentSort);
        model.addAttribute("board", board);
        model.addAttribute("comments", commentPageData.getContent());
        model.addAttribute("commentPage", commentPageData.getNumber());
        model.addAttribute("commentTotalPages", commentPageData.getTotalPages());
        model.addAttribute("commentPageRange", buildCommentPageRange(commentPageData.getNumber(), commentPageData.getTotalPages()));
        model.addAttribute("commentSort", commentSort);
        model.addAttribute("currentUser", currentUser);
        return "fragments/comments :: commentsList";
    }

    // 댓글 작성
    @PostMapping("/{boardId}/comments")
    public String createComment(@PathVariable Long boardId,
                                 @RequestParam String content,
                                 @RequestParam(required = false) Long parentId,
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
            commentService.createComment(board, content, currentUser, parentId);
        } catch (Exception e) {
            log.error("Failed to create comment: {}", e.getMessage());
        }

        return "redirect:/board/" + boardId;
    }

    // 댓글 수정
    @PostMapping("/comments/{commentId}/edit")
    public String updateComment(@PathVariable Long commentId,
                                 @RequestParam String content,
                                 @RequestParam Long boardId,
                                 HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/board";
        }

        try {
            commentService.updateComment(commentId, content, currentUser);
        } catch (Exception e) {
            log.error("Failed to update comment: {}", e.getMessage());
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

    // 첨부파일 다운로드
    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id, HttpSession session) {
        boolean isLoggedIn = session.getAttribute("currentUser") != null
                || session.getAttribute("adminUser") != null;
        if (!isLoggedIn) {
            return ResponseEntity.status(401).build();
        }

        BoardAttachment attachment = boardAttachmentService.getById(id);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }

        // 관리자는 만료된 파일도 다운로드 가능
        boolean isAdmin = session.getAttribute("adminUser") != null;
        if (!isAdmin && attachment.isExpired()) {
            return ResponseEntity.status(410).build();
        }

        Resource resource = boardAttachmentService.getAttachmentFile(id);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        String encodedFilename;
        try {
            encodedFilename = URLEncoder.encode(attachment.getOriginalName(), "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            encodedFilename = attachment.getStoredName();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    /**
     * 사이드바 카테고리 정렬: [전사](이름순) > [부서](deptOrder순) > 없음(이름순)
     */
    /**
     * 페이지네이션 표시 범위 계산. -1은 생략 부호(...)를 의미.
     * 최대 8개 요소: [첫] [...] [창4개] [...] [끝]
     * 예) 현재=9, 전체=20 → [0, -1, 7, 8, 9, 10, -1, 19]
     */
    private List<Integer> buildCommentPageRange(int currentPage, int totalPages) {
        List<Integer> range = new ArrayList<>();
        if (totalPages <= 8) {
            for (int i = 0; i < totalPages; i++) range.add(i);
            return range;
        }
        // 현재 페이지 중심의 4칸 슬라이딩 창
        int windowSize = 4;
        int from = Math.max(0, currentPage - windowSize / 2);
        int to   = Math.min(totalPages - 1, from + windowSize - 1);
        from = Math.max(0, to - windowSize + 1); // to가 끝에 붙으면 from 재조정
        if (from > 0) {
            range.add(0);
            if (from > 1) range.add(-1);
        }
        for (int i = from; i <= to; i++) range.add(i);
        if (to < totalPages - 1) {
            if (to < totalPages - 2) range.add(-1);
            range.add(totalPages - 1);
        }
        return range;
    }

    private List<BoardCategory> buildSortedCategories(User currentUser) {
        // [전사]: deptIds 비어있음, 이름순
        List<BoardCategory> companyWide = boardCategoryService.getCompanyWideCategories();
        companyWide.sort(Comparator.comparing(BoardCategory::getName));

        // 부서 관련 (deptIds 있음): getDeptCategoriesForUser()가 deptOrder 순으로 반환
        List<BoardCategory> deptCategories = boardCategoryService.getDeptCategoriesForUser(currentUser.getDeptId());

        // [부서]: deptId != null (스케줄러 자동생성), deptOrder 순 유지
        // 없음: deptId == null (관리자 수동생성+부서지정), 이름순
        List<BoardCategory> schedulerDept = new ArrayList<>();
        List<BoardCategory> adminDept = new ArrayList<>();
        for (BoardCategory c : deptCategories) {
            if (c.getDeptId() != null) schedulerDept.add(c);
            else adminDept.add(c);
        }
        adminDept.sort(Comparator.comparing(BoardCategory::getName));

        List<BoardCategory> result = new ArrayList<>();
        result.addAll(companyWide);
        result.addAll(schedulerDept);
        result.addAll(adminDept);
        return result;
    }
}
