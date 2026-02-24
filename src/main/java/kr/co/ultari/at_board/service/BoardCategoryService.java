package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.repository.primary.BoardCategoryRepository;
import kr.co.ultari.at_board.repository.primary.BoardLikeRepository;
import kr.co.ultari.at_board.repository.primary.BoardRepository;
import kr.co.ultari.at_board.repository.primary.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardCategoryService {

    private final BoardCategoryRepository boardCategoryRepository;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final DepartmentService departmentService;

    public BoardCategory getCategoryById(Long id) {
        return boardCategoryRepository.findById(id).orElse(null);
    }

    public BoardCategory getCategoryByDeptId(String deptId) {
        List<BoardCategory> list = boardCategoryRepository.findByDeptIdAndIsActiveTrue(deptId);
        return list.isEmpty() ? null : list.get(0);
    }

    // 활성/비활성 관계없이 해당 부서 게시판이 존재하는지 확인 (스케줄러용)
    public BoardCategory getCategoryByDeptIdAny(String deptId) {
        List<BoardCategory> list = boardCategoryRepository.findByDeptId(deptId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<BoardCategory> getAllCategories() {
        return boardCategoryRepository.findAll();
    }

    public List<BoardCategory> getActiveCategories() {
        return boardCategoryRepository.findByIsActiveTrue();
    }

    // 전사 공용 카테고리 (deptIds 비어 있음)
    public List<BoardCategory> getCompanyWideCategories() {
        return boardCategoryRepository.findCompanyWideAndActive();
    }

    /**
     * 사용자 부서 및 모든 상위 부서에 해당하는 활성 부서 게시판 목록 반환.
     */
    public List<BoardCategory> getDeptCategoriesForUser(String userDeptId) {
        Set<String> ancestorDeptIds = departmentService.getSelfAndAncestorDeptIds(userDeptId);
        if (ancestorDeptIds.isEmpty()) return new ArrayList<>();
        return boardCategoryRepository.findByDeptIdsInAndIsActiveTrue(ancestorDeptIds);
    }

    @Transactional("primaryTransactionManager")
    public BoardCategory createCategory(BoardCategory category) {
        return boardCategoryRepository.save(category);
    }

    @Transactional("primaryTransactionManager")
    public BoardCategory updateCategory(Long id, BoardCategory updatedCategory) {
        BoardCategory category = boardCategoryRepository.findById(id).orElse(null);
        if (category != null) {
            category.setName(updatedCategory.getName());
            category.setDescription(updatedCategory.getDescription());
            category.setIsActive(updatedCategory.getIsActive());
            category.setAdminOnly(updatedCategory.getAdminOnly());
            category.setDeptIds(updatedCategory.getDeptIds());
            return boardCategoryRepository.save(category);
        }
        return null;
    }

    @Transactional("primaryTransactionManager")
    public boolean deleteCategory(Long id) {
        BoardCategory category = boardCategoryRepository.findById(id).orElse(null);
        if (category == null) return false;

        log.info("Deleting category {} with bulk delete", category.getName());

        // 벌크 삭제: 댓글 → 좋아요 → 게시글 순서로 삭제 (N+1 방지)
        commentRepository.deleteByBoardCategory(category);
        boardLikeRepository.deleteByBoardCategory(category);
        boardRepository.deleteByCategory(category);

        // 카테고리 삭제 (board_category_depts도 자동 삭제)
        boardCategoryRepository.deleteById(id);
        return true;
    }

    public long getTotalCategoryCount() {
        return boardCategoryRepository.count();
    }

    // 어드민: 전체 카테고리 페이징
    public Page<BoardCategory> getAllCategoriesPaged(Pageable pageable) {
        return boardCategoryRepository.findAllByOrderByIdDesc(pageable);
    }

    // 어드민: 이름 검색 + 페이징
    public Page<BoardCategory> searchCategoriesByName(String keyword, Pageable pageable) {
        return boardCategoryRepository.findByNameContainingIgnoreCaseOrderByIdDesc(keyword, pageable);
    }
}
