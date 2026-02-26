package kr.co.ultari.at_board.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "database.table")
@Data
public class DatabaseTableConfig {

    private UserTable user = new UserTable();
    private DeptTable dept = new DeptTable();

    @Data
    public static class UserTable {
        private String name = "MSG_USER";
        private String userId = "USER_ID";
        private String userName = "USER_NAME";
        private String posName = "POS_NAME";
        private String deptId = "DEPT_ID";
        private String userOrder = "";
    }

    @Data
    public static class DeptTable {
        private String name = "MSG_DEPT";
        private String deptId = "DEPT_ID";
        private String deptName = "DEPT_NAME";
        private String parentDept = "PARENT_DEPT";
        private String deptOrder = "";
    }
}
