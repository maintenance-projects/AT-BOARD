package kr.co.ultari.at_board.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.context.ApplicationContext;

public class CustomPhysicalNamingStrategy implements PhysicalNamingStrategy {

    private static ApplicationContext applicationContext;
    private static DatabaseTableConfig tableConfig;

    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
        if (context != null) {
            tableConfig = context.getBean(DatabaseTableConfig.class);
        }
    }

    @Override
    public Identifier toPhysicalCatalogName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return identifier;
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return identifier;
    }

    @Override
    public Identifier toPhysicalTableName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        if (identifier == null || tableConfig == null) {
            return identifier;
        }

        String name = identifier.getText();

        // User 테이블
        if ("User".equals(name)) {
            return Identifier.toIdentifier(tableConfig.getUser().getName());
        }

        // Department 테이블
        if ("Department".equals(name)) {
            return Identifier.toIdentifier(tableConfig.getDept().getName());
        }

        return identifier;
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return identifier;
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        if (identifier == null || tableConfig == null) {
            return identifier;
        }

        String name = identifier.getText();

        // User 테이블 컬럼 매핑
        if ("userId".equals(name)) {
            return Identifier.toIdentifier(tableConfig.getUser().getUserId());
        }
        if ("userName".equals(name)) {
            return Identifier.toIdentifier(tableConfig.getUser().getUserName());
        }
        if ("posName".equals(name)) {
            return Identifier.toIdentifier(tableConfig.getUser().getPosName());
        }
        if ("deptId".equals(name)) {
            return Identifier.toIdentifier(tableConfig.getUser().getDeptId());
        }

        // Department 테이블 컬럼 매핑
        if ("deptId".equals(name)) {
            return Identifier.toIdentifier(tableConfig.getDept().getDeptId());
        }
        if ("deptName".equals(name)) {
            return Identifier.toIdentifier(tableConfig.getDept().getDeptName());
        }

        return identifier;
    }
}
