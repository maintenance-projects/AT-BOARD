package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.BoardTemplateSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BoardTemplateSectionRepository extends JpaRepository<BoardTemplateSection, Long> {
    List<BoardTemplateSection> findAllByOrderBySortOrderAscIdAsc();
}
