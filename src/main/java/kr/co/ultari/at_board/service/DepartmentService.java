package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.model.secondary.Dept;
import kr.co.ultari.at_board.repository.primary.BoardCategoryRepository;
import kr.co.ultari.at_board.repository.secondary.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    /**
     * [부서] 카테고리 정렬용 composite 키 반환 (deptId → "DDDDD_order").
     * 트리 레벨(depth) 오름차순, 같은 레벨은 deptOrder 오름차순.
     * findAll() 1회 쿼리로 depth 계산 + deptOrder 조회를 동시 처리.
     */
    public Map<String, String> getDeptCompositeSortKeyMap(Collection<String> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return Collections.emptyMap();

        List<Dept> allDepts = departmentRepository.findAll();
        Map<String, String> parentMap = new HashMap<>();
        Map<String, String> orderMap = new HashMap<>();
        for (Dept d : allDepts) {
            parentMap.put(d.getDeptId(), d.getParentDept());
            String order = d.getDeptOrder();
            orderMap.put(d.getDeptId(), (order != null && !order.isEmpty()) ? order : "999999");
        }

        Map<String, String> result = new HashMap<>();
        for (String deptId : deptIds) {
            int depth = 0;
            String current = deptId;
            Set<String> visited = new HashSet<>();
            while (current != null && visited.add(current) && parentMap.containsKey(current)) {
                String parent = parentMap.get(current);
                if (parent == null || parent.isEmpty() || "0".equals(parent)) break;
                depth++;
                current = parent;
            }
            String order = orderMap.getOrDefault(deptId, "999999");
            result.put(deptId, String.format("%05d_%s", depth, order));
        }
        return result;
    }

    /**
     * 특정 부서 ID의 자신 + 모든 하위 부서 ID 집합 반환 (알림 수신자 결정용).
     * findAll() 1회 쿼리로 childMap 구성 후 BFS 탐색 (cycle guard 포함).
     */
    public Set<String> getSelfAndDescendantDeptIds(String deptId) {
        if (deptId == null || deptId.isEmpty()) return new HashSet<>();
        List<Dept> allDepts = departmentRepository.findAll();
        Map<String, List<String>> childMap = new HashMap<>();
        for (Dept d : allDepts) {
            if (d.getParentDept() != null && !d.getParentDept().isEmpty() && !"0".equals(d.getParentDept())) {
                childMap.computeIfAbsent(d.getParentDept(), k -> new java.util.ArrayList<>()).add(d.getDeptId());
            }
        }
        Set<String> result = new HashSet<>();
        java.util.Queue<String> queue = new java.util.LinkedList<>();
        queue.add(deptId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!result.add(current)) continue; // cycle guard
            List<String> children = childMap.get(current);
            if (children != null) queue.addAll(children);
        }
        return result;
    }

    /**
     * 여러 부서 ID 각각의 자신+하위 부서 ID 합집합 반환 (알림 수신자 결정용).
     */
    public Set<String> getSelfAndAllDescendantDeptIds(Set<String> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return new HashSet<>();
        List<Dept> allDepts = departmentRepository.findAll();
        Map<String, List<String>> childMap = new HashMap<>();
        for (Dept d : allDepts) {
            if (d.getParentDept() != null && !d.getParentDept().isEmpty() && !"0".equals(d.getParentDept())) {
                childMap.computeIfAbsent(d.getParentDept(), k -> new java.util.ArrayList<>()).add(d.getDeptId());
            }
        }
        Set<String> result = new HashSet<>();
        java.util.Queue<String> queue = new java.util.LinkedList<>();
        queue.addAll(deptIds);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!result.add(current)) continue; // cycle guard
            List<String> children = childMap.get(current);
            if (children != null) queue.addAll(children);
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
