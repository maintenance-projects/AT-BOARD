package kr.co.ultari.at_board.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.jpa")
@Data
public class HibernatePropertiesConfig {

    private HibernateSettings primary = new HibernateSettings();
    private HibernateSettings secondary = new HibernateSettings();

    @Data
    public static class HibernateSettings {
        private HibernateConfig hibernate = new HibernateConfig();
    }

    @Data
    public static class HibernateConfig {
        private String ddlAuto = "update";
        private String dialect = "org.hibernate.dialect.MariaDBDialect";
        private boolean showSql = true;
        private boolean formatSql = true;
    }
}
