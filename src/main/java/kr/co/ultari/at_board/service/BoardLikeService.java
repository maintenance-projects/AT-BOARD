package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.BoardLike;
import kr.co.ultari.at_board.repository.primary.BoardLikeRepository;
import kr.co.ultari.at_board.repository.primary.BoardRepository;
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
public class BoardLikeService {

    private final BoardLikeRepository boardLikeRepository;
    private final BoardRepository boardRepository;

    public boolean isLiked(Long boardId, String userId) {
        return boardLikeRepository.existsByBoardIdAndUserId(boardId, userId);
    }

    public Set<Long> getLikedBoardIds(List<Long> boardIds, String userId) {
        if (boardIds == null || boardIds.isEmpty()) return Collections.emptySet();
        return new HashSet<>(boardLikeRepository.findLikedBoardIds(userId, boardIds));
    }

    @Transactional("primaryTransactionManager")
    public boolean toggleLike(Long boardId, String userId) {
        boolean exists = boardLikeRepository.existsByBoardIdAndUserId(boardId, userId);

        if (exists) {
            // 좋아요 취소: 실제 삭제된 건수 확인 후 카운트 감소 (동시 취소 방어)
            int deleted = boardLikeRepository.deleteByBoardIdAndUserId(boardId, userId);
            if (deleted > 0) {
                boardRepository.decrementLikeCount(boardId);
            }
            return false;
        } else {
            // 좋아요 추가: 원자적 카운트 증가
            BoardLike like = BoardLike.builder()
                    .boardId(boardId)
                    .userId(userId)
                    .build();
            boardLikeRepository.save(like);
            boardRepository.incrementLikeCount(boardId);
            return true;
        }
    }
}
