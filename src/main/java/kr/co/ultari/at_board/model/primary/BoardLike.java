package kr.co.ultari.at_board.model.primary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "board_likes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(BoardLike.BoardLikeId.class)
public class BoardLike {

    @Id
    @Column(name = "BOARD_ID", nullable = false)
    private Long boardId;

    @Id
    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 복합 키 클래스
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoardLikeId implements Serializable {
        private Long boardId;
        private String userId;
    }
}
