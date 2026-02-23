package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.AdminRole;
import kr.co.ultari.at_board.model.primary.Admin;
import kr.co.ultari.at_board.repository.primary.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional("primaryTransactionManager")
    public void initAdmin() {
        if (adminRepository.count() == 0) {
            Admin admin = Admin.builder()
                    .adminId("admin")
                    .password(passwordEncoder.encode("admin1234"))
                    .adminName("최고관리자")
                    .role(AdminRole.SUPER_ADMIN)
                    .build();
            adminRepository.save(admin);
            log.info("============================================================");
            log.info("초기 관리자 계정이 생성되었습니다.");
            log.info("ID: admin");
            log.info("Password: admin1234");
            log.info("보안을 위해 로그인 후 비밀번호를 변경하세요.");
            log.info("============================================================");
        }
    }
}
