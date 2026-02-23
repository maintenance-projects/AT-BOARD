package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.BoardCategory;
import kr.co.ultari.at_board.repository.primary.BoardCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardCategoryService {

    private final BoardCategoryRepository boardCategoryRepository;

    public BoardCategory getCategoryById(Long id) {
        return boardCategoryRepository.findById(id).orElse(null);
    }

    public BoardCategory getCategoryByDeptId(String deptId) {
        return boardCategoryRepository.findByDeptId(deptId).orElse(null);
    }

    public List<BoardCategory> getAllCategories() {
        return boardCategoryRepository.findAll();
    }

    public List<BoardCategory> getActiveCategories() {
        return boardCategoryRepository.findByIsActiveTrue();
    }

    public List<BoardCategory> getCompanyWideCategories() {
        return boardCategoryRepository.findByDeptIdIsNull();
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
            return boardCategoryRepository.save(category);
        }
        return null;
    }

    @Transactional("primaryTransactionManager")
    public boolean deleteCategory(Long id) {
        if (boardCategoryRepository.existsById(id)) {
            boardCategoryRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public long getTotalCategoryCount() {
        return boardCategoryRepository.count();
    }
}
