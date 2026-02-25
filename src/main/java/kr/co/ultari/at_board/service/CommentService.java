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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final DepartmentService departmentService;

    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public Set<Long> getCommentedBoardIds(List<Long> boardIds, String userId) {
        if (boardIds == null || boardIds.isEmpty()) return Collections.emptySet();
        return new HashSet<>(commentRepository.findCommentedBoardIds(userId, boardIds));
    }

    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public List<Comment> getCommentsByBoard(Board board) {
        // 루트 댓글만 조회 후 각 댓글의 답댓글을 populate
        List<Comment> rootComments = commentRepository.findByBoardAndParentIdIsNullOrderByCreatedAtAsc(board);
        for (Comment comment : rootComments) {
            List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(comment.getId());
            comment.setReplies(replies);
        }
        return rootComments;
    }

    @Transactional("primaryTransactionManager")
    public Comment createComment(Board board, String content, User author, Long parentId) {
        Comment comment = Comment.builder()
                .board(board)
                .userId(author.getUserId())
                .authorName(author.getUserName())
                .authorPosName(author.getPosName())
                .authorDeptName(author.getDeptName() != null
                        ? author.getDeptName()
                        : departmentService.getDeptName(author.getDeptId()))
                .content(content)
                .parentId(parentId)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // 게시물의 댓글 수 업데이트 (답댓글 포함)
        board.setCommentCount(board.getCommentCount() + 1);
        boardRepository.save(board);

        return savedComment;
    }

    @Transactional("primaryTransactionManager")
    public Comment updateComment(Long commentId, String content, User currentUser) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) return null;

        if (!comment.getUserId().equals(currentUser.getUserId())) {
            log.warn("Access denied: User {} tried to edit comment {} owned by {}",
                    currentUser.getUserId(), commentId, comment.getUserId());
            throw new IllegalArgumentException("본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        comment.setContent(content);
        return commentRepository.save(comment);
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

            // 루트 댓글이면 답댓글도 함께 삭제
            int deleteCount = 1;
            if (comment.getParentId() == null) {
                List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
                deleteCount += replies.size();
                for (Comment reply : replies) {
                    commentRepository.deleteById(reply.getId());
                }
            }
            commentRepository.deleteById(commentId);

            // 게시물의 댓글 수 업데이트
            board.setCommentCount(Math.max(0, board.getCommentCount() - deleteCount));
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

            // 루트 댓글이면 답댓글도 함께 삭제
            int deleteCount = 1;
            if (comment.getParentId() == null) {
                List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
                deleteCount += replies.size();
                for (Comment reply : replies) {
                    commentRepository.deleteById(reply.getId());
                }
            }
            commentRepository.deleteById(commentId);

            // 게시물의 댓글 수 업데이트
            board.setCommentCount(Math.max(0, board.getCommentCount() - deleteCount));
            boardRepository.save(board);

            return true;
        }
        return false;
    }
}
