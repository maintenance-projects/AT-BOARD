package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.secondary.Department;
import kr.co.ultari.at_board.repository.primary.BoardCategoryRepository;
import kr.co.ultari.at_board.repository.secondary.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final BoardCategoryRepository boardCategoryRepository;

    @Transactional("secondaryTransactionManager")
    public Department createOrGetDepartment(String deptId, String deptName) {
        return departmentRepository.findByDeptId(deptId)
                .orElseGet(() -> {
                    log.info("Creating new department: {} ({})", deptName, deptId);

                    // 부서 생성
                    Department dept = Department.builder()
                            .deptId(deptId)
                            .deptName(deptName)
                            .build();
                    departmentRepository.save(dept);

                    // 부서별 게시판 자동 생성 (Primary DB에 저장)
                    createBoardCategoryForDepartment(deptId, deptName);

                    return dept;
                });
    }

    @Transactional("primaryTransactionManager")
    public void createBoardCategoryForDepartment(String deptId, String deptName) {
        BoardCategory category = BoardCategory.builder()
                .name(deptName + " 게시판")
                .description(deptName + " 전용 게시판입니다.")
                .deptId(deptId)
                .deptName(deptName)
                .isActive(true)
                .build();
        boardCategoryRepository.save(category);

        log.info("Created board category for department: {}", category.getName());
    }
}
