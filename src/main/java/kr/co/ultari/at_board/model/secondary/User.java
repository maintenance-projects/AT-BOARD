package kr.co.ultari.at_board.model.secondary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "MSG_USER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String userId;

    private String userName;

    private String posName;

    private String deptId;

    private String userOrder;

    // DB에 저장하지 않고 로그인 시 부서 테이블에서 조회하여 채움
    @javax.persistence.Transient
    private String deptName;

    // 이름 + 직책명 조합
    public String getDisplayName() {
        if (posName != null && !posName.isEmpty()) {
            return userName + " " + posName;
        }
        return userName;
    }
}
