package kr.co.ultari.at_board.repository.secondary;

import kr.co.ultari.at_board.model.secondary.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUserId(String userId);
    List<User> findByDeptIdIn(Collection<String> deptIds);

    @Query("SELECT u.userId FROM User u")
    List<String> findAllUserIds();

    @Query("SELECT u.userId FROM User u WHERE u.deptId IN :deptIds")
    List<String> findUserIdsByDeptIdIn(@Param("deptIds") Collection<String> deptIds);
}
