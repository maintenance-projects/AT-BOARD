package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.BoardAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BoardAttachmentRepository extends JpaRepository<BoardAttachment, Long> {

    List<BoardAttachment> findByBoardId(Long boardId);

    @Transactional("primaryTransactionManager")
    void deleteAllByBoardId(Long boardId);
}
