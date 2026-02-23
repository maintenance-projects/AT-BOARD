package kr.co.ultari.at_board.model.secondary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "User")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String userId;

    private String userName;

    private String posName;

    // Department 객체 대신 String으로 deptId만 저장 (다른 DB이므로 FK 불가)
    private String deptId;

    // 이름 + 직책명 조합
    public String getDisplayName() {
        if (posName != null && !posName.isEmpty()) {
            return userName + " " + posName;
        }
        return userName;
    }
}
