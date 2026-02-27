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
@Table(name = "board", indexes = {
        // 카테고리별 최신순 조회 (가장 빈번한 쿼리)
        @Index(name = "idx_board_category_created", columnList = "CATEGORY_ID, CREATED_AT"),
        // 전체 최신순 조회 (어드민)
        @Index(name = "idx_board_created_at", columnList = "CREATED_AT"),
        // 작성자 조회
        @Index(name = "idx_board_user_id", columnList = "USER_ID")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TITLE", nullable = false, length = 100,
            columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String title;

    @Column(name = "CONTENT", nullable = false,
            columnDefinition = "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String content;

    // User 객체 참조 대신 userId만 저장 (다른 DB이므로 FK 불가)
    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Column(name = "AUTHOR_NAME", nullable = false, length = 100,
            columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String authorName;

    @Column(name = "AUTHOR_POS_NAME", length = 100,
            columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String authorPosName;

    @Column(name = "AUTHOR_DEPT_NAME", length = 100,
            columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String authorDeptName;

    // BoardCategory는 같은 DB이므로 FK 가능
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CATEGORY_ID", nullable = false)
    private BoardCategory category;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "VIEW_COUNT", nullable = false)
    private int viewCount;

    @Column(name = "LIKE_COUNT", nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Column(name = "COMMENT_COUNT", nullable = false)
    @Builder.Default
    private int commentCount = 0;

    // 부서명 + 이름 + 직책명
    public String getAuthorName() {
        StringBuilder sb = new StringBuilder();
        if (authorDeptName != null && !authorDeptName.isEmpty()) {
            sb.append(authorDeptName).append(" ");
        }
        sb.append(authorName);
        if (authorPosName != null && !authorPosName.isEmpty()) {
            sb.append(" ").append(authorPosName);
        }
        return sb.toString();
    }

    // 이름만 반환
    public String getAuthorNameOnly() {
        return authorName;
    }

    // 생성 시각 (epoch ms, JS용)
    public long getCreatedAtEpochMilli() {
        if (createdAt == null) return 0;
        return createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // 이미지 포함 여부
    public boolean isHasImage() {
        if (content == null) return false;
        return content.contains("<img");
    }

    // HTML 태그 제거한 순수 텍스트 (미리보기용)
    public String getPlainContent() {
        if (content == null) return "";
        // HTML 태그 제거
        String plain = content.replaceAll("<[^>]*>", "");
        // HTML 엔티티 디코딩
        plain = plain.replaceAll("&nbsp;", " ");
        plain = plain.replaceAll("&lt;", "<");
        plain = plain.replaceAll("&gt;", ">");
        plain = plain.replaceAll("&amp;", "&");
        plain = plain.replaceAll("&quot;", "\"");
        // 연속된 공백 정리
        plain = plain.replaceAll("\\s+", " ");
        return plain.trim();
    }
}
