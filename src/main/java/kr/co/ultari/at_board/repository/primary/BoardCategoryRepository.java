package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.BoardCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BoardCategoryRepository extends JpaRepository<BoardCategory, Long> {

    // 전사 공용 (deptIds 비어 있음)
    @Query("SELECT c FROM BoardCategory c WHERE c.deptIds IS EMPTY AND c.isActive = true")
    List<BoardCategory> findCompanyWideAndActive();

    @Query("SELECT c FROM BoardCategory c WHERE c.deptIds IS EMPTY")
    List<BoardCategory> findCompanyWide();

    // 부서 전용 - 특정 deptId 포함 여부 (isActive 무관) - 다중 카테고리 가능
    @Query("SELECT c FROM BoardCategory c JOIN c.deptIds d WHERE d = :deptId")
    List<BoardCategory> findByDeptId(@Param("deptId") String deptId);

    // 부서 전용 - 특정 deptId 포함 & 활성 - 다중 카테고리 가능
    @Query("SELECT c FROM BoardCategory c JOIN c.deptIds d WHERE d = :deptId AND c.isActive = true")
    List<BoardCategory> findByDeptIdAndIsActiveTrue(@Param("deptId") String deptId);

    // 사용자 접근 가능 카테고리: 카테고리의 deptIds 중 하나라도 ancestorDeptIds에 포함되면 반환
    @Query("SELECT DISTINCT c FROM BoardCategory c JOIN c.deptIds d WHERE d IN :ancestorDeptIds AND c.isActive = true")
    List<BoardCategory> findByDeptIdsInAndIsActiveTrue(@Param("ancestorDeptIds") Collection<String> ancestorDeptIds);

    // isActive
    @Query("SELECT c FROM BoardCategory c WHERE c.isActive = true")
    List<BoardCategory> findByIsActiveTrue();

    // 스케줄러 동기화용: 카테고리에 등록된 모든 deptId 목록 (1회 쿼리로 N+1 방지)
    @Query("SELECT d FROM BoardCategory c JOIN c.deptIds d")
    List<String> findAllExistingDeptIds();

    // 어드민: 전체 페이징
    Page<BoardCategory> findAllByOrderByIdDesc(Pageable pageable);

    // 어드민: 이름 검색 + 페이징
    Page<BoardCategory> findByNameContainingIgnoreCaseOrderByIdDesc(String keyword, Pageable pageable);
}
