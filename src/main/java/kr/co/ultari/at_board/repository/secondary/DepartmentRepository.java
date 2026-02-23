package kr.co.ultari.at_board.repository.secondary;

import kr.co.ultari.at_board.model.secondary.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {
    Optional<Department> findByDeptId(String deptId);
}
