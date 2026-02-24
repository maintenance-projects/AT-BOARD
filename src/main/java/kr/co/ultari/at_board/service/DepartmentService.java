package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.secondary.Dept;
import kr.co.ultari.at_board.repository.primary.BoardCategoryRepository;
import kr.co.ultari.at_board.repository.secondary.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final BoardCategoryRepository boardCategoryRepository;

    @Transactional("secondaryTransactionManager")
    public Dept createOrGetDepartment(String deptId, String deptName) {
        return departmentRepository.findByDeptId(deptId)
                .orElseGet(() -> {
                    log.info("Creating new department: {} ({})", deptName, deptId);

                    Dept dept = Dept.builder()
                            .deptId(deptId)
                            .deptName(deptName)
                            .build();
                    departmentRepository.save(dept);

                    createBoardCategoryForDepartment(deptId, deptName);

                    return dept;
                });
    }

    /**
     * 사용자 부서가 대상 부서들 중 하나 또는 그 하위 부서인지 확인 (다중 부서 지원).
     */
    public boolean isInAnyDeptOrSubDept(String userDeptId, Set<String> targetDeptIds) {
        if (userDeptId == null || targetDeptIds == null || targetDeptIds.isEmpty()) return false;
        String current = userDeptId;
        Set<String> visited = new LinkedHashSet<>();
        while (current != null && !visited.contains(current)) {
            if (targetDeptIds.contains(current)) return true;
            visited.add(current);
            Dept dept = departmentRepository.findByDeptId(current).orElse(null);
            if (dept == null || dept.getParentDept() == null || dept.getParentDept().isEmpty()) break;
            current = dept.getParentDept();
        }
        return false;
    }

    /**
     * 사용자 부서가 대상 부서 또는 그 하위 부서인지 확인 (단일 부서).
     */
    public boolean isInDeptOrSubDept(String userDeptId, String targetDeptId) {
        if (targetDeptId == null) return false;
        return isInAnyDeptOrSubDept(userDeptId, Collections.singleton(targetDeptId));
    }

    /**
     * 사용자 부서 ID를 포함한 모든 상위 부서 ID 집합 반환 (자신 포함, 루트까지).
     */
    public Set<String> getSelfAndAncestorDeptIds(String deptId) {
        Set<String> result = new LinkedHashSet<>();
        if (deptId == null) return result;
        String current = deptId;
        while (current != null && !result.contains(current)) {
            result.add(current);
            Dept dept = departmentRepository.findByDeptId(current).orElse(null);
            if (dept == null || dept.getParentDept() == null || dept.getParentDept().isEmpty()) break;
            current = dept.getParentDept();
        }
        return result;
    }

    public String getDeptName(String deptId) {
        if (deptId == null) return null;
        return departmentRepository.findByDeptId(deptId)
                .map(Dept::getDeptName)
                .orElse(null);
    }

    @Transactional("primaryTransactionManager")
    public void createBoardCategoryForDepartment(String deptId, String deptName) {
        Set<String> deptIds = new LinkedHashSet<>();
        deptIds.add(deptId);

        BoardCategory category = BoardCategory.builder()
                .name(deptName + " 게시판")
                .description(deptName + " 전용 게시판입니다.")
                .deptId(deptId)
                .deptName(deptName)
                .deptIds(deptIds)
                .isActive(true)
                .build();
        boardCategoryRepository.save(category);

        log.info("Created board category for department: {}", category.getName());
    }
}
