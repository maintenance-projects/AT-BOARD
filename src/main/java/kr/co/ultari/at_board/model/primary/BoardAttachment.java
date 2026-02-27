package kr.co.ultari.at_board.model.primary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "board_attachments", indexes = {
        @Index(name = "idx_attachment_board_id", columnList = "BOARD_ID")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "BOARD_ID", nullable = true)
    private Long boardId;

    @Column(name = "ORIGINAL_NAME", nullable = false, length = 500,
            columnDefinition = "VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String originalName;

    @Column(name = "STORED_NAME", nullable = false, length = 500)
    private String storedName;

    @Column(name = "FILE_PATH", nullable = false, length = 1000,
            columnDefinition = "VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String filePath;

    @Column(name = "FILE_SIZE", nullable = false)
    private Long fileSize;

    @Column(name = "MIME_TYPE", length = 200)
    private String mimeType;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    // 파일 크기를 사람이 읽기 좋은 형식으로 반환
    public String getFileSizeDisplay() {
        if (fileSize == null) return "0 B";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }
}
