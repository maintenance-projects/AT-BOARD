package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.primary.Comment;
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

    // 카테고리 내 전체 댓글 벌크 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.board IN (SELECT b FROM Board b WHERE b.category = :category)")
    void deleteByBoardCategory(@Param("category") BoardCategory category);
}
