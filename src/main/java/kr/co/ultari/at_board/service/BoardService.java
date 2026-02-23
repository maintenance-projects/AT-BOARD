package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.secondary.User;
import kr.co.ultari.at_board.repository.primary.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardCategoryService boardCategoryService;

    public List<Board> getAllBoards() {
        return boardRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Board> getBoardsByUser(User user) {
        // 사용자의 부서 게시판 + 전사 공용 게시판 조회
        List<BoardCategory> categories = new ArrayList<>();

        // 부서 게시판
        BoardCategory deptCategory = boardCategoryService.getCategoryByDeptId(user.getDeptId());
        if (deptCategory != null) {
            categories.add(deptCategory);
        }

        // 전사 공용 게시판
        categories.addAll(boardCategoryService.getCompanyWideCategories());

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

    @Transactional("primaryTransactionManager")
    public Board getBoardById(Long id) {
        Board board = boardRepository.findById(id).orElse(null);
        if (board != null) {
            board.setViewCount(board.getViewCount() + 1);
            boardRepository.save(board);
        }
        return board;
    }

    public Board getBoardByIdForUser(Long id, User user) {
        Board board = getBoardById(id);
        if (board != null) {
            // 권한 체크: 부서 게시판이면 같은 부서만 조회 가능
            BoardCategory category = board.getCategory();
            if (category.getDeptId() != null &&
                    !category.getDeptId().equals(user.getDeptId())) {
                log.warn("Access denied: User {} tried to access board {} from different department",
                        user.getUserId(), id);
                return null;
            }
        }
        return board;
    }

    @Transactional("primaryTransactionManager")
    public Board createBoard(String title, String content, User author, BoardCategory category) {
        // 권한 체크: 부서 게시판이면 같은 부서만 글 작성 가능
        if (category.getDeptId() != null &&
                !category.getDeptId().equals(author.getDeptId())) {
            log.warn("Access denied: User {} tried to write to category {} from different department",
                    author.getUserId(), category.getId());
            throw new IllegalArgumentException("해당 게시판에 글을 작성할 권한이 없습니다.");
        }

        Board board = Board.builder()
                .title(title)
                .content(content)
                .userId(author.getUserId())
                .authorName(author.getUserName())
                .authorPosName(author.getPosName())
                .category(category)
                .viewCount(0)
                .build();

        return boardRepository.save(board);
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

            boardRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // 관리자용 메서드
    @Transactional("primaryTransactionManager")
    public Board createBoardByAdmin(Board board) {
        return boardRepository.save(board);
    }

    @Transactional("primaryTransactionManager")
    public boolean deleteBoardByAdmin(Long id) {
        if (boardRepository.existsById(id)) {
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
}
