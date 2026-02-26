package kr.co.ultari.at_board.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.context.ApplicationContext;

public class CustomPhysicalNamingStrategy implements PhysicalNamingStrategy {

    private static ApplicationContext applicationContext;
    private static DatabaseTableConfig tableConfig;

    // toPhysicalTableName → toPhysicalColumnName 순서 보장을 이용해
    // 현재 처리 중인 엔티티 컨텍스트를 추적 (Hibernate 메타데이터 빌드 단계에서만 사용)
    private static final ThreadLocal<String> entityContext = new ThreadLocal<>();

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

        // User 테이블 — 컨텍스트 기록 후 매핑
        if ("MSG_USER".equals(name)) {
            entityContext.set("USER");
            return Identifier.toIdentifier(tableConfig.getUser().getName());
        }

        // Dept 테이블 — 컨텍스트 기록 후 매핑
        if ("MSG_DEPT".equals(name)) {
            entityContext.set("DEPT");
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
        String ctx = entityContext.get();

        // User 테이블 전용 컬럼
        if ("userId".equals(name))   return Identifier.toIdentifier(tableConfig.getUser().getUserId());
        if ("userName".equals(name)) return Identifier.toIdentifier(tableConfig.getUser().getUserName());
        if ("posName".equals(name))  return Identifier.toIdentifier(tableConfig.getUser().getPosName());

        // deptId: User와 Dept 양쪽에 존재하므로 엔티티 컨텍스트로 구분
        if ("deptId".equals(name)) {
            if ("DEPT".equals(ctx)) {
                return Identifier.toIdentifier(tableConfig.getDept().getDeptId());
            }
            return Identifier.toIdentifier(tableConfig.getUser().getDeptId());
        }

        // Dept 테이블 전용 컬럼
        if ("deptName".equals(name))   return Identifier.toIdentifier(tableConfig.getDept().getDeptName());
        if ("parentDept".equals(name)) return Identifier.toIdentifier(tableConfig.getDept().getParentDept());
        if ("deptOrder".equals(name)) {
            String col = tableConfig.getDept().getDeptOrder();
            return (col != null && !col.isEmpty()) ? Identifier.toIdentifier(col) : identifier;
        }

        // User 테이블 전용 컬럼 (정렬)
        if ("userOrder".equals(name)) {
            String col = tableConfig.getUser().getUserOrder();
            return (col != null && !col.isEmpty()) ? Identifier.toIdentifier(col) : identifier;
        }

        return identifier;
    }
}
