package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.Admin;
import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.Comment;
import kr.co.ultari.at_board.model.secondary.User;
import kr.co.ultari.at_board.repository.primary.BoardRepository;
import kr.co.ultari.at_board.repository.primary.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        // 1회 쿼리로 모든 댓글 조회 후 인-메모리에서 트리 구성 (N+1 방지)
        List<Comment> allComments = commentRepository.findByBoardOrderByCreatedAtAsc(board);
        Map<Long, Comment> commentMap = new LinkedHashMap<>();
        List<Comment> rootComments = new ArrayList<>();
        for (Comment comment : allComments) {
            comment.setReplies(new ArrayList<>());
            commentMap.put(comment.getId(), comment);
            if (comment.getParentId() == null) {
                rootComments.add(comment);
            }
        }
        for (Comment comment : allComments) {
            if (comment.getParentId() != null) {
                Comment parent = commentMap.get(comment.getParentId());
                if (parent != null) {
                    parent.getReplies().add(comment);
                }
            }
        }
        return rootComments;
    }

    public static final int COMMENT_PAGE_SIZE = 10;

    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public Page<Comment> getCommentsByBoardPaged(Board board, int page, String sort) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sort) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), COMMENT_PAGE_SIZE,
                Sort.by(direction, "createdAt"));

        // 요청 페이지가 범위를 벗어날 경우 마지막 페이지로 보정
        Page<Comment> rootPage = commentRepository.findRootCommentsByBoard(board, pageRequest);
        if (rootPage.getTotalPages() > 0 && page >= rootPage.getTotalPages()) {
            rootPage = commentRepository.findRootCommentsByBoard(board,
                    PageRequest.of(rootPage.getTotalPages() - 1, COMMENT_PAGE_SIZE,
                            Sort.by(direction, "createdAt")));
        }

        List<Long> rootIds = new ArrayList<>();
        for (Comment c : rootPage.getContent()) {
            rootIds.add(c.getId());
        }
        if (!rootIds.isEmpty()) {
            List<Comment> replies = commentRepository.findByParentIdIn(rootIds);
            Map<Long, List<Comment>> replyMap = new HashMap<>();
            for (Comment reply : replies) {
                List<Comment> list = replyMap.get(reply.getParentId());
                if (list == null) {
                    list = new ArrayList<>();
                    replyMap.put(reply.getParentId(), list);
                }
                list.add(reply);
            }
            for (Comment root : rootPage.getContent()) {
                List<Comment> replyList = replyMap.get(root.getId());
                root.setReplies(replyList != null ? replyList : new ArrayList<Comment>());
            }
        }
        return rootPage;
    }

    @Transactional("primaryTransactionManager")
    public Comment createCommentByAdmin(Board board, String content, Admin admin) {
        Comment comment = Comment.builder()
                .board(board)
                .userId("admin_" + admin.getAdminId())
                .authorName(admin.getAdminName())
                .authorPosName("관리자")
                .content(content)
                .build();
        Comment savedComment = commentRepository.save(comment);
        boardRepository.incrementCommentCount(board.getId());
        return savedComment;
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

        // 댓글 수 원자적 증가 (read-modify-save 경쟁 조건 방지)
        boardRepository.incrementCommentCount(board.getId());

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

            // 루트 댓글이면 답댓글도 벌크 삭제 (N+1 방지)
            int deleteCount = 1;
            if (comment.getParentId() == null) {
                int replyCount = (int) commentRepository.countByParentId(commentId);
                if (replyCount > 0) {
                    commentRepository.deleteByParentId(commentId);
                    deleteCount += replyCount;
                }
            }
            commentRepository.deleteById(commentId);

            // 댓글 수 원자적 감소 (read-modify-save 경쟁 조건 방지)
            boardRepository.decrementCommentCount(board.getId(), deleteCount);

            return true;
        }
        return false;
    }

    @Transactional("primaryTransactionManager")
    public boolean deleteCommentByAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null) {
            Board board = comment.getBoard();

            // 루트 댓글이면 답댓글도 벌크 삭제 (N+1 방지)
            int deleteCount = 1;
            if (comment.getParentId() == null) {
                int replyCount = (int) commentRepository.countByParentId(commentId);
                if (replyCount > 0) {
                    commentRepository.deleteByParentId(commentId);
                    deleteCount += replyCount;
                }
            }
            commentRepository.deleteById(commentId);

            // 댓글 수 원자적 감소 (read-modify-save 경쟁 조건 방지)
            boardRepository.decrementCommentCount(board.getId(), deleteCount);

            return true;
        }
        return false;
    }
}
