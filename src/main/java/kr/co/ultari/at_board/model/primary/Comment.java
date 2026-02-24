package kr.co.ultari.at_board.model.primary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments", indexes = {
        // 게시글별 댓글 최신순 (BOARD_ID는 FK로 인덱스 있으나 복합으로 추가)
        @Index(name = "idx_comment_board_created", columnList = "BOARD_ID, CREATED_AT"),
        // 사용자별 댓글 조회
        @Index(name = "idx_comment_user_id", columnList = "USER_ID")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BOARD_ID", nullable = false)
    private Board board;

    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Column(name = "AUTHOR_NAME", nullable = false, length = 100)
    private String authorName;

    @Column(name = "AUTHOR_POS_NAME", length = 100)
    private String authorPosName;

    @Column(name = "AUTHOR_DEPT_NAME", length = 100)
    private String authorDeptName;

    @Column(name = "CONTENT", nullable = false, length = 1000)
    private String content;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 부서명 + 이름 + 직책명 조합
    public String getAuthorDisplayName() {
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
}
