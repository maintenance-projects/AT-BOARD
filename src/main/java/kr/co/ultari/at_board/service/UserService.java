package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.secondary.Department;
import kr.co.ultari.at_board.model.secondary.User;
import kr.co.ultari.at_board.repository.secondary.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentService departmentService;

    @Transactional("secondaryTransactionManager")
    public User processUserLogin(String userId) {
        // 사용자 조회 (DB에 없으면 null 반환)
        User user = userRepository.findByUserId(userId).orElse(null);

        if (user == null) {
            log.warn("User not found in database: {}", userId);
            return null;
        }

        log.info("User logged in: {} {} (Dept: {}, UserId: {})",
                user.getUserName(),
                user.getPosName() != null ? user.getPosName() : "",
                user.getDeptId(),
                user.getUserId());
        return user;
    }

    @Transactional("secondaryTransactionManager")
    public User processUserLogin(String userId, String userName, String posName, String deptId, String deptName) {
        // 부서 조회 또는 생성 (부서별 게시판도 자동 생성됨)
        Department department = departmentService.createOrGetDepartment(deptId, deptName);

        // 사용자 조회 또는 생성
        User user = userRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating new user: {} {} ({})", userName, posName != null ? posName : "", userId);
                    User newUser = User.builder()
                            .userId(userId)
                            .userName(userName)
                            .posName(posName)
                            .deptId(deptId)
                            .build();
                    return userRepository.save(newUser);
                });

        // 정보 업데이트 (이름이나 직책이 변경될 수 있음)
        boolean updated = false;
        if (!userName.equals(user.getUserName())) {
            user.setUserName(userName);
            updated = true;
        }
        if (posName != null && !posName.equals(user.getPosName())) {
            user.setPosName(posName);
            updated = true;
        }
        if (deptId != null && !deptId.equals(user.getDeptId())) {
            user.setDeptId(deptId);
            updated = true;
        }
        if (updated) {
            userRepository.save(user);
        }

        log.info("User logged in: {} {} (Dept: {}, UserId: {})",
                user.getUserName(),
                user.getPosName() != null ? user.getPosName() : "",
                user.getDeptId(),
                user.getUserId());
        return user;
    }

    public User getUserById(String id) {
        return userRepository.findById(id).orElse(null);
    }

    public User getUserByUserId(String userId) {
        return userRepository.findByUserId(userId).orElse(null);
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }
}
