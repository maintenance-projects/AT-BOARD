package kr.co.ultari.at_board.scheduler;

import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.secondary.Dept;
import kr.co.ultari.at_board.repository.secondary.DepartmentRepository;
import kr.co.ultari.at_board.service.BoardCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 부서 테이블을 주기적으로 확인하여 부서별 게시판을 자동 생성하는 스케줄러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DepartmentSyncScheduler {

    private final DepartmentRepository departmentRepository;
    private final BoardCategoryService boardCategoryService;

    /**
     * 매시간 정각에 부서-게시판 동기화 실행
     * cron: 초 분 시 일 월 요일
     * "0 0 * * * *" = 매시간 정각 (0분 0초)
     */
    @Scheduled(cron = "${scheduler.department-sync.cron:0 0 * * * *}")
    @Transactional("primaryTransactionManager")
    public void syncDepartmentCategories() {
        log.info("=== 부서-게시판 동기화 시작 ===");

        try {
            // secondary DB에서 모든 부서 조회
            List<Dept> departments = departmentRepository.findAll();
            log.info("부서 테이블에서 {} 개의 부서 조회", departments.size());

            int createdCount = 0;
            int skippedCount = 0;

            for (Dept dept : departments) {
                // parentDept가 "0"인 최상위 부서(회사)는 게시판 생성 제외
                if ("0".equals(dept.getParentDept())) {
                    log.debug("최상위 부서 스킵: {} ({})", dept.getDeptName(), dept.getDeptId());
                    skippedCount++;
                    continue;
                }

                // 해당 부서의 게시판이 이미 존재하는지 확인
                BoardCategory existingCategory = boardCategoryService.getCategoryByDeptIdAny(dept.getDeptId());

                if (existingCategory == null) {
                    // 게시판이 없으면 새로 생성
                    java.util.Set<String> newDeptIds = new java.util.HashSet<>();
                    newDeptIds.add(dept.getDeptId());
                    BoardCategory newCategory = BoardCategory.builder()
                            .name(dept.getDeptName() + " 게시판")
                            .description(dept.getDeptName() + " 전용 게시판입니다.")
                            .deptId(dept.getDeptId())
                            .deptName(dept.getDeptName())
                            .deptIds(newDeptIds)
                            .isActive(true)
                            .adminOnly(false)
                            .build();

                    boardCategoryService.createCategory(newCategory);
                    log.info("신규 부서 게시판 생성: {} (부서ID: {})", newCategory.getName(), dept.getDeptId());
                    createdCount++;
                } else {
                    skippedCount++;
                }
            }

            log.info("=== 부서-게시판 동기화 완료 === 생성: {}, 건너뜀: {}", createdCount, skippedCount);

        } catch (Exception e) {
            log.error("부서-게시판 동기화 중 오류 발생", e);
        }
    }

    /**
     * 애플리케이션 시작 시 1회 실행 (초기 동기화)
     */
    @Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE)
    @Transactional("primaryTransactionManager")
    public void initialSync() {
        log.info("=== 초기 부서-게시판 동기화 실행 ===");
        syncDepartmentCategories();
    }
}
