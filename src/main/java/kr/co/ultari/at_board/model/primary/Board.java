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
@Table(name = "board")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TITLE", nullable = false, length = 100)
    private String title;

    @Column(name = "CONTENT", nullable = false, columnDefinition = "TEXT")
    private String content;

    // User 객체 참조 대신 userId만 저장 (다른 DB이므로 FK 불가)
    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Column(name = "AUTHOR_NAME", nullable = false, length = 100)
    private String authorName;

    @Column(name = "AUTHOR_POS_NAME", length = 100)
    private String authorPosName;

    // BoardCategory는 같은 DB이므로 FK 가능
    @ManyToOne(fetch = FetchType.LAZY)
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

    // 템플릿 호환성을 위한 메서드 (이름 + 직책명)
    public String getAuthorName() {
        if (authorPosName != null && !authorPosName.isEmpty()) {
            return authorName + " " + authorPosName;
        }
        return authorName;
    }

    // 이름만 반환
    public String getAuthorNameOnly() {
        return authorName;
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
