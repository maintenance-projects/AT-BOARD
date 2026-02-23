package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.Admin;
import kr.co.ultari.at_board.repository.primary.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional("primaryTransactionManager")
    public Admin authenticateAdmin(String adminId, String password) {
        Admin admin = adminRepository.findByAdminId(adminId).orElse(null);

        if (admin == null) {
            log.warn("Admin not found: {}", adminId);
            return null;
        }

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            log.warn("Invalid password for admin: {}", adminId);
            return null;
        }

        // lastLoginAt 업데이트
        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);

        log.info("Admin logged in: {}", adminId);
        return admin;
    }

    public Admin getAdminById(Long id) {
        return adminRepository.findById(id).orElse(null);
    }

    @Transactional("primaryTransactionManager")
    public Admin createAdmin(Admin admin) {
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return adminRepository.save(admin);
    }

    public long getAdminCount() {
        return adminRepository.count();
    }
}
