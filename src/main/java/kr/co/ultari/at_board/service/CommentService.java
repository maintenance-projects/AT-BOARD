package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.Comment;
import kr.co.ultari.at_board.model.secondary.User;
import kr.co.ultari.at_board.repository.primary.BoardRepository;
import kr.co.ultari.at_board.repository.primary.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;

    public List<Comment> getCommentsByBoard(Board board) {
        return commentRepository.findByBoardOrderByCreatedAtAsc(board);
    }

    @Transactional("primaryTransactionManager")
    public Comment createComment(Board board, String content, User author) {
        Comment comment = Comment.builder()
                .board(board)
                .userId(author.getUserId())
                .authorName(author.getUserName())
                .authorPosName(author.getPosName())
                .content(content)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // 게시물의 댓글 수 업데이트
        board.setCommentCount(board.getCommentCount() + 1);
        boardRepository.save(board);

        return savedComment;
    }

    @Transactional("primaryTransactionManager")
    public boolean deleteComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null) {
            // 권한 체크: 본인만 삭제 가능
            if (!comment.getUserId().equals(currentUser.getUserId())) {
                log.warn("Access denied: User {} tried to delete comment {} owned by {}",
                        currentUser.getUserId(), commentId, comment.getUserId());
                throw new IllegalArgumentException("본인이 작성한 댓글만 삭제할 수 있습니다.");
            }

            Board board = comment.getBoard();
            commentRepository.deleteById(commentId);

            // 게시물의 댓글 수 업데이트
            board.setCommentCount(Math.max(0, board.getCommentCount() - 1));
            boardRepository.save(board);

            return true;
        }
        return false;
    }

    @Transactional("primaryTransactionManager")
    public boolean deleteCommentByAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null) {
            Board board = comment.getBoard();
            commentRepository.deleteById(commentId);

            // 게시물의 댓글 수 업데이트
            board.setCommentCount(Math.max(0, board.getCommentCount() - 1));
            boardRepository.save(board);

            return true;
        }
        return false;
    }
}
