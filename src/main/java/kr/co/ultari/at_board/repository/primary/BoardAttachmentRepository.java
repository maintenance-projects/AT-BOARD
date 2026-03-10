package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.BoardAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface BoardAttachmentRepository extends JpaRepository<BoardAttachment, Long> {

    List<BoardAttachment> findByBoardId(Long boardId);

    @Transactional("primaryTransactionManager")
    void deleteAllByBoardId(Long boardId);

    @Query("SELECT DISTINCT a.boardId FROM BoardAttachment a WHERE a.boardId IN :boardIds")
    List<Long> findBoardIdsWithAttachments(@Param("boardIds") List<Long> boardIds);

    @Query("SELECT a FROM BoardAttachment a WHERE a.expiresAt IS NOT NULL AND a.expiresAt < :now AND a.boardId IS NOT NULL")
    List<BoardAttachment> findExpiredWithFile(@Param("now") LocalDateTime now);

    // 첨부파일 일괄 게시글 할당 (boardId=null인 것만) - N+1 방지 벌크 UPDATE
    @Modifying
    @Query("UPDATE BoardAttachment a SET a.boardId = :boardId WHERE a.id IN :ids AND a.boardId IS NULL")
    void assignBoardIdByIds(@Param("boardId") Long boardId, @Param("ids") List<Long> ids);

    // 여러 게시글의 첨부파일 조회/삭제 (카테고리 삭제 시 벌크 처리)
    @Query("SELECT a FROM BoardAttachment a WHERE a.boardId IN :boardIds")
    List<BoardAttachment> findByBoardIdIn(@Param("boardIds") List<Long> boardIds);

    @Modifying
    @Transactional("primaryTransactionManager")
    @Query("DELETE FROM BoardAttachment a WHERE a.boardId IN :boardIds")
    void deleteAllByBoardIdIn(@Param("boardIds") List<Long> boardIds);
}
