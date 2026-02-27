package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.secondary.User;
import kr.co.ultari.at_board.repository.primary.BoardLikeRepository;
import kr.co.ultari.at_board.repository.primary.BoardRepository;
import kr.co.ultari.at_board.repository.primary.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardCategoryService boardCategoryService;
    private final DepartmentService departmentService;
    private final CommentRepository commentRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final NotificationService notificationService;

    public List<Board> getAllBoards() {
        return boardRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Board> getBoardsByUser(User user) {
        List<BoardCategory> categories = new ArrayList<>();
        categories.addAll(boardCategoryService.getCompanyWideCategories());
        categories.addAll(boardCategoryService.getDeptCategoriesForUser(user.getDeptId()));
        return boardRepository.findByCategoryInOrderByCreatedAtDesc(categories);
    }

    public List<Board> getBoardsByCategoryId(Long categoryId) {
        BoardCategory category = boardCategoryService.getCategoryById(categoryId);
        if (category == null) {
            return new ArrayList<>();
        }
        return boardRepository.findByCategoryOrderByCreatedAtDesc(category);
    }

    public Page<Board> getBoardsByCategoryIdPaged(Long categoryId, Pageable pageable) {
        BoardCategory category = boardCategoryService.getCategoryById(categoryId);
        if (category == null) {
            return Page.empty(pageable);
        }
        return boardRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
    }

    public Page<Board> searchBoards(Long categoryId, String searchType, String keyword, Pageable pageable) {
        BoardCategory category = boardCategoryService.getCategoryById(categoryId);
        if (category == null) {
            return Page.empty(pageable);
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return boardRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
        }
        if ("title".equals(searchType)) {
            return boardRepository.findByCategoryAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(category, keyword, pageable);
        } else if ("content".equals(searchType)) {
            return boardRepository.findByCategoryAndContentContainingIgnoreCaseOrderByCreatedAtDesc(category, keyword, pageable);
        } else if ("author".equals(searchType)) {
            return boardRepository.findByCategoryAndAuthorNameContainingIgnoreCaseOrderByCreatedAtDesc(category, keyword, pageable);
        } else {
            return boardRepository.searchByCategoryAndTitleOrContent(category, keyword, pageable);
        }
    }

    @Transactional("primaryTransactionManager")
    public Board getBoardById(Long id) {
        Board board = boardRepository.findById(id).orElse(null);
        if (board != null) {
            // 원자적 업데이트로 동시 접근 시 Race condition 방지
            boardRepository.incrementViewCount(id);
            board.setViewCount(board.getViewCount() + 1);
        }
        return board;
    }

    public Board getBoardByIdNoCount(Long id) {
        return boardRepository.findById(id).orElse(null);
    }

    @Transactional("primaryTransactionManager")
    public Board getBoardByIdForUser(Long id, User user) {
        Board board = getBoardById(id);
        if (board != null) {
            // 권한 체크: 부서 게시판이면 해당 부서 또는 하위 부서 사용자만 조회 가능
            BoardCategory category = board.getCategory();
            if (category != null && !category.getDeptIds().isEmpty() &&
                    !departmentService.isInAnyDeptOrSubDept(user.getDeptId(), category.getDeptIds())) {
                log.warn("Access denied: User {} tried to access board {} (depts={})",
                        user.getUserId(), id, category.getDeptIds());
                return null;
            }
        }
        return board;
    }

    // 조회수 증가 없이 권한 체크만 수행 (세션 내 중복 조회 시 사용)
    @Transactional("primaryTransactionManager")
    public Board getBoardByIdForUserNoCount(Long id, User user) {
        Board board = getBoardByIdNoCount(id);
        if (board != null) {
            BoardCategory category = board.getCategory();
            if (category != null && !category.getDeptIds().isEmpty() &&
                    !departmentService.isInAnyDeptOrSubDept(user.getDeptId(), category.getDeptIds())) {
                return null;
            }
        }
        return board;
    }

    @Transactional("primaryTransactionManager")
    public Board createBoard(String title, String content, User author, BoardCategory category) {
        // 권한 체크: 부서 게시판이면 해당 부서 또는 하위 부서 사용자만 글 작성 가능
        if (!category.getDeptIds().isEmpty() &&
                !departmentService.isInAnyDeptOrSubDept(author.getDeptId(), category.getDeptIds())) {
            log.warn("Access denied: User {} tried to write to category {} (depts={})",
                    author.getUserId(), category.getId(), category.getDeptIds());
            throw new IllegalArgumentException("해당 게시판에 글을 작성할 권한이 없습니다.");
        }

        Board board = Board.builder()
                .title(title)
                .content(content)
                .userId(author.getUserId())
                .authorName(author.getUserName())
                .authorPosName(author.getPosName())
                .authorDeptName(author.getDeptName() != null
                        ? author.getDeptName()
                        : departmentService.getDeptName(author.getDeptId()))
                .category(category)
                .viewCount(0)
                .build();

        Board saved = boardRepository.save(board);
        notificationService.notifyNewPost(saved);
        return saved;
    }

    @Transactional("primaryTransactionManager")
    public Board updateBoard(Long id, String title, String content, User currentUser) {
        Board board = boardRepository.findById(id).orElse(null);
        if (board != null) {
            // 권한 체크: 본인만 수정 가능
            if (!board.getUserId().equals(currentUser.getUserId())) {
                log.warn("Access denied: User {} tried to edit board {} owned by {}",
                        currentUser.getUserId(), id, board.getUserId());
                throw new IllegalArgumentException("본인이 작성한 글만 수정할 수 있습니다.");
            }

            board.setTitle(title);
            board.setContent(content);
            return boardRepository.save(board);
        }
        return null;
    }

    @Transactional("primaryTransactionManager")
    public boolean deleteBoard(Long id, User currentUser) {
        Board board = boardRepository.findById(id).orElse(null);
        if (board != null) {
            // 권한 체크: 본인만 삭제 가능
            if (!board.getUserId().equals(currentUser.getUserId())) {
                log.warn("Access denied: User {} tried to delete board {} owned by {}",
                        currentUser.getUserId(), id, board.getUserId());
                throw new IllegalArgumentException("본인이 작성한 글만 삭제할 수 있습니다.");
            }

            // FK 제약 조건 순서: 댓글 → 좋아요 → 게시글
            commentRepository.deleteByBoardId(id);
            boardLikeRepository.deleteByBoardId(id);
            boardRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // 어드민: 전체/카테고리별 페이징+검색
    public Page<Board> adminSearchBoards(Long categoryId, String searchType, String keyword, Pageable pageable) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        if (categoryId != null) {
            return searchBoards(categoryId, searchType, keyword, pageable);
        }
        if (!hasKeyword) {
            return boardRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        if ("title".equals(searchType)) {
            return boardRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword, pageable);
        } else if ("content".equals(searchType)) {
            return boardRepository.findByContentContainingIgnoreCaseOrderByCreatedAtDesc(keyword, pageable);
        } else if ("author".equals(searchType)) {
            return boardRepository.findByAuthorNameContainingIgnoreCaseOrderByCreatedAtDesc(keyword, pageable);
        } else {
            return boardRepository.searchAllByTitleOrContent(keyword, pageable);
        }
    }

    // 관리자용 메서드
    @Transactional("primaryTransactionManager")
    public Board createBoardByAdmin(Board board) {
        Board saved = boardRepository.save(board);
        notificationService.notifyNewPost(saved);
        return saved;
    }

    @Transactional("primaryTransactionManager")
    public boolean deleteBoardByAdmin(Long id) {
        if (boardRepository.existsById(id)) {
            // FK 제약 조건 순서: 댓글 → 좋아요 → 게시글
            commentRepository.deleteByBoardId(id);
            boardLikeRepository.deleteByBoardId(id);
            boardRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public long getTotalBoardCount() {
        return boardRepository.count();
    }

    public long getTodayBoardCount() {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        return boardRepository.countByCreatedAtAfter(startOfDay);
    }

    // 카테고리별 신규 게시글 ID 맵 (N 배지용, 최근 24시간)
    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public Map<String, List<Long>> getNewPostsByCategoryMap(LocalDateTime since) {
        List<Object[]> rows = boardRepository.findNewPostIdAndCategoryIdSince(since);
        Map<String, List<Long>> result = new HashMap<>();
        for (Object[] row : rows) {
            Long postId = (Long) row[0];
            Long catId = (Long) row[1];
            String key = String.valueOf(catId);
            if (!result.containsKey(key)) {
                result.put(key, new ArrayList<>());
            }
            result.get(key).add(postId);
        }
        return result;
    }
}
