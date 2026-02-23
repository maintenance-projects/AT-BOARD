package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.BoardLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardLikeRepository extends JpaRepository<BoardLike, BoardLike.BoardLikeId> {
    long countByBoardId(Long boardId);

    boolean existsByBoardIdAndUserId(Long boardId, String userId);

    void deleteByBoardIdAndUserId(Long boardId, String userId);
}
