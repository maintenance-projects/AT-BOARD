package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.primary.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByBoardOrderByCreatedAtAsc(Board board);

    // 루트 댓글만 (parentId IS NULL)
    List<Comment> findByBoardAndParentIdIsNullOrderByCreatedAtAsc(Board board);

    // 루트 댓글 페이징
    Page<Comment> findByBoardAndParentIdIsNullOrderByCreatedAtAsc(Board board, Pageable pageable);

    // 루트 댓글 페이징 (정렬 가변 - Pageable의 Sort 사용)
    @Query("SELECT c FROM Comment c WHERE c.board = :board AND c.parentId IS NULL")
    Page<Comment> findRootCommentsByBoard(@Param("board") Board board, Pageable pageable);

    // 여러 루트 댓글의 답댓글 일괄 조회 (N+1 방지)
    @Query("SELECT c FROM Comment c WHERE c.parentId IN :parentIds ORDER BY c.createdAt ASC")
    List<Comment> findByParentIdIn(@Param("parentIds") List<Long> parentIds);

    // 특정 댓글의 답댓글
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    // 게시글의 모든 답댓글 (parentId IS NOT NULL) - 삭제 시 사용
    List<Comment> findByBoardAndParentIdIsNotNull(Board board);

    void deleteByBoard(Board board);

    long countByBoard(Board board);

    List<Comment> findByUserId(String userId);

    // 현재 페이지 게시글 중 사용자가 댓글 단 boardId 목록
    @Query("SELECT DISTINCT c.board.id FROM Comment c WHERE c.userId = :userId AND c.board.id IN :boardIds")
    List<Long> findCommentedBoardIds(@Param("userId") String userId, @Param("boardIds") List<Long> boardIds);

    // 답댓글 수 조회 (루트 댓글 삭제 시 감소량 계산)
    long countByParentId(Long parentId);

    // 루트 댓글의 답댓글 벌크 삭제 (N+1 방지)
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.parentId = :parentId")
    void deleteByParentId(@Param("parentId") Long parentId);

    // 게시글 삭제 시 댓글 벌크 삭제 (FK 제약 조건 해소)
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.board.id = :boardId")
    void deleteByBoardId(@Param("boardId") Long boardId);

    // 카테고리 내 전체 댓글 벌크 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.board IN (SELECT b FROM Board b WHERE b.category = :category)")
    void deleteByBoardCategory(@Param("category") BoardCategory category);
}
