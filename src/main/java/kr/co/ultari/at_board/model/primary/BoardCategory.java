package kr.co.ultari.at_board.model.primary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import org.hibernate.annotations.BatchSize;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "board_categories", indexes = {
        @Index(name = "idx_category_is_active", columnList = "IS_ACTIVE")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    // 단일 부서 정보 (스케줄러 자동 생성 게시판용, 다중 부서 선택 시 null)
    @Column(name = "DEPT_ID", length = 50)
    private String deptId;

    @Column(name = "DEPT_NAME", length = 100)
    private String deptName;

    // 다중 부서 지원: board_category_depts 조인 테이블
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "board_category_depts",
            joinColumns = @JoinColumn(name = "category_id"),
            indexes = @Index(name = "idx_bcd_dept_id", columnList = "dept_id"))
    @Column(name = "dept_id", length = 50)
    @BatchSize(size = 100)
    @Builder.Default
    private Set<String> deptIds = new HashSet<>();

    @Column(name = "IS_ACTIVE", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "ADMIN_ONLY", nullable = false)
    @Builder.Default
    private Boolean adminOnly = false;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    /** 부서 전용 여부 */
    @Transient
    public boolean isDeptSpecific() {
        return deptIds != null && !deptIds.isEmpty();
    }
}
