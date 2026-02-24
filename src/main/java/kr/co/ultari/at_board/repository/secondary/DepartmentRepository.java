package kr.co.ultari.at_board.repository.secondary;

import kr.co.ultari.at_board.model.secondary.Dept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Dept, String> {
    Optional<Dept> findByDeptId(String deptId);
}
