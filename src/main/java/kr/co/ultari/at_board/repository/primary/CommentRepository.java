package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByBoardOrderByCreatedAtAsc(Board board);

    long countByBoard(Board board);

    List<Comment> findByUserId(String userId);
}
