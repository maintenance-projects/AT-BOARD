package kr.co.ultari.at_board.model.primary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
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

    @Column(name = "CONTENT", nullable = false, length = 1000)
    private String content;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 이름 + 직책명 조합
    public String getAuthorDisplayName() {
        if (authorPosName != null && !authorPosName.isEmpty()) {
            return authorName + " " + authorPosName;
        }
        return authorName;
    }
}
