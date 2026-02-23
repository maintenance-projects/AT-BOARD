package kr.co.ultari.at_board.model.primary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "board_categories")
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

    // Department 객체 참조 대신 deptId만 저장 (다른 DB이므로 FK 불가)
    @Column(name = "DEPT_ID", length = 50)
    private String deptId;

    @Column(name = "DEPT_NAME", length = 100)
    private String deptName;

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
}
