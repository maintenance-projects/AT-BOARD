package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.BoardLike;
import kr.co.ultari.at_board.repository.primary.BoardLikeRepository;
import kr.co.ultari.at_board.repository.primary.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardLikeService {

    private final BoardLikeRepository boardLikeRepository;
    private final BoardRepository boardRepository;

    public boolean isLiked(Long boardId, String userId) {
        return boardLikeRepository.existsByBoardIdAndUserId(boardId, userId);
    }

    @Transactional("primaryTransactionManager")
    public boolean toggleLike(Long boardId, String userId) {
        boolean exists = boardLikeRepository.existsByBoardIdAndUserId(boardId, userId);
        Board board = boardRepository.findById(boardId).orElse(null);

        if (board == null) {
            return false;
        }

        if (exists) {
            // 좋아요 취소
            boardLikeRepository.deleteByBoardIdAndUserId(boardId, userId);
            board.setLikeCount(Math.max(0, board.getLikeCount() - 1));
            boardRepository.save(board);
            return false;
        } else {
            // 좋아요 추가
            BoardLike like = BoardLike.builder()
                    .boardId(boardId)
                    .userId(userId)
                    .build();
            boardLikeRepository.save(like);
            board.setLikeCount(board.getLikeCount() + 1);
            boardRepository.save(board);
            return true;
        }
    }
}
