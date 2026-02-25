package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.Board;
import kr.co.ultari.at_board.model.primary.BoardCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // category를 JOIN FETCH로 함께 로드 (LazyInitializationException 방지)
    @EntityGraph(attributePaths = {"category"})
    List<Board> findAllByOrderByCreatedAtDesc();

    List<Board> findByCategoryInOrderByCreatedAtDesc(List<BoardCategory> categories);

    @EntityGraph(attributePaths = {"category"})
    List<Board> findByCategoryOrderByCreatedAtDesc(BoardCategory category);

    // 페이징 지원 (user-facing board list API - category 접근 없음, EntityGraph 불필요)
    Page<Board> findByCategoryOrderByCreatedAtDesc(BoardCategory category, Pageable pageable);

    // findById 오버라이드: category JOIN FETCH (admin 상세 페이지용)
    @Override
    @EntityGraph(attributePaths = {"category"})
    Optional<Board> findById(Long id);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    List<Board> findByUserId(String userId);

    // 조회수 원자적 증가 (Race condition 방지)
    @Modifying
    @Query("UPDATE Board b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // 카테고리 내 게시글 전체 벌크 삭제 (카테고리 삭제 시)
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Board b WHERE b.category = :category")
    void deleteByCategory(@Param("category") BoardCategory category);

    // 검색: 제목
    Page<Board> findByCategoryAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(BoardCategory category, String keyword, Pageable pageable);

    // 검색: 내용
    Page<Board> findByCategoryAndContentContainingIgnoreCaseOrderByCreatedAtDesc(BoardCategory category, String keyword, Pageable pageable);

    // 검색: 제목+내용
    @Query("SELECT b FROM Board b WHERE b.category = :category AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY b.createdAt DESC")
    Page<Board> searchByCategoryAndTitleOrContent(@Param("category") BoardCategory category, @Param("keyword") String keyword, Pageable pageable);

    // 검색: 작성자
    Page<Board> findByCategoryAndAuthorNameContainingIgnoreCaseOrderByCreatedAtDesc(BoardCategory category, String keyword, Pageable pageable);

    // 신규 게시글 ID + 카테고리 ID (N 배지용)
    @Query("SELECT b.id, b.category.id FROM Board b WHERE b.createdAt > :since")
    List<Object[]> findNewPostIdAndCategoryIdSince(@Param("since") LocalDateTime since);

    // 어드민: 전체 게시글 페이징
    @EntityGraph(attributePaths = {"category"})
    Page<Board> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 어드민: 전체 게시글 검색 (카테고리 무관)
    @EntityGraph(attributePaths = {"category"})
    Page<Board> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Board> findByContentContainingIgnoreCaseOrderByCreatedAtDesc(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Board> findByAuthorNameContainingIgnoreCaseOrderByCreatedAtDesc(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT b FROM Board b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY b.createdAt DESC")
    Page<Board> searchAllByTitleOrContent(@Param("keyword") String keyword, Pageable pageable);
}
