package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.primary.BoardLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardLikeRepository extends JpaRepository<BoardLike, BoardLike.BoardLikeId> {
    long countByBoardId(Long boardId);

    // 현재 페이지 게시글 중 사용자가 좋아요한 boardId 목록
    @Query("SELECT bl.boardId FROM BoardLike bl WHERE bl.userId = :userId AND bl.boardId IN :boardIds")
    List<Long> findLikedBoardIds(@Param("userId") String userId, @Param("boardIds") List<Long> boardIds);

    boolean existsByBoardIdAndUserId(Long boardId, String userId);

    // 실제 삭제 건수 반환 - 0이면 이미 삭제됨 (동시 요청 방어)
    @Transactional
    @Modifying
    @Query("DELETE FROM BoardLike bl WHERE bl.boardId = :boardId AND bl.userId = :userId")
    int deleteByBoardIdAndUserId(@Param("boardId") Long boardId, @Param("userId") String userId);

    void deleteByBoardId(Long boardId);

    // 카테고리 내 전체 좋아요 벌크 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM BoardLike bl WHERE bl.boardId IN (SELECT b.id FROM Board b WHERE b.category = :category)")
    void deleteByBoardCategory(@Param("category") BoardCategory category);
}
