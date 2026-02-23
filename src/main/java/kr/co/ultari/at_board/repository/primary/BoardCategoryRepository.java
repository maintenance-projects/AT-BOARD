package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.BoardCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardCategoryRepository extends JpaRepository<BoardCategory, Long> {
    List<BoardCategory> findByDeptIdIsNull();
    Optional<BoardCategory> findByDeptId(String deptId);
    List<BoardCategory> findByIsActiveTrue();
}
