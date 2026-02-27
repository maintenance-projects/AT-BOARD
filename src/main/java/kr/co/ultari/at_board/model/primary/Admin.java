package kr.co.ultari.at_board.model.primary;

import kr.co.ultari.at_board.model.AdminRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admins")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ADMIN_ID", nullable = false, unique = true, length = 50)
    private String adminId;

    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;

    @Column(name = "ADMIN_NAME", nullable = false, length = 100,
            columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String adminName;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    @Builder.Default
    private AdminRole role = AdminRole.ADMIN;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;
}
