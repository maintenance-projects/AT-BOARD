package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.BoardTemplate;
import kr.co.ultari.at_board.model.primary.BoardTemplateSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BoardTemplateRepository extends JpaRepository<BoardTemplate, Long> {

    @Query("SELECT t FROM BoardTemplate t LEFT JOIN t.section s " +
           "WHERE t.isActive = true " +
           "ORDER BY COALESCE(s.sortOrder, 9999) ASC, COALESCE(s.id, 9999999) ASC, " +
           "COALESCE(t.sortOrder, 9999) ASC, t.name ASC")
    List<BoardTemplate> findActiveTemplatesOrdered();

    @Query("SELECT t FROM BoardTemplate t LEFT JOIN t.section s " +
           "ORDER BY COALESCE(s.sortOrder, 9999) ASC, COALESCE(s.id, 9999999) ASC, " +
           "COALESCE(t.sortOrder, 9999) ASC, t.name ASC")
    List<BoardTemplate> findAllOrderedWithSection();

    List<BoardTemplate> findBySectionOrderBySortOrderAscNameAsc(BoardTemplateSection section);

    List<BoardTemplate> findBySectionIsNullOrderBySortOrderAscNameAsc();

    boolean existsBySection(BoardTemplateSection section);
}
