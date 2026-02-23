package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.BoardCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findAllByOrderByCreatedAtDesc();

    List<Board> findByCategoryInOrderByCreatedAtDesc(List<BoardCategory> categories);

    List<Board> findByCategoryOrderByCreatedAtDesc(BoardCategory category);

    // 페이징 지원
    Page<Board> findByCategoryOrderByCreatedAtDesc(BoardCategory category, Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    List<Board> findByUserId(String userId);
}
