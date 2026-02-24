package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.primary.BoardLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardLikeRepository extends JpaRepository<BoardLike, BoardLike.BoardLikeId> {
    long countByBoardId(Long boardId);

    boolean existsByBoardIdAndUserId(Long boardId, String userId);

    void deleteByBoardIdAndUserId(Long boardId, String userId);

    void deleteByBoardId(Long boardId);

    // 카테고리 내 전체 좋아요 벌크 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM BoardLike bl WHERE bl.boardId IN (SELECT b.id FROM Board b WHERE b.category = :category)")
    void deleteByBoardCategory(@Param("category") BoardCategory category);
}
