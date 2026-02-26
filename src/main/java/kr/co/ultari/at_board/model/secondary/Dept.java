package kr.co.ultari.at_board.model.secondary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "MSG_DEPT")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dept {

    @Id
    private String deptId;

    private String deptName;

    private String parentDept;

    private String deptOrder;
}