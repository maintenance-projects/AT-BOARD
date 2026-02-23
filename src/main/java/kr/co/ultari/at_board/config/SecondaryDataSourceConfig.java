package kr.co.ultari.at_board.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "kr.co.ultari.at_board.repository.secondary",
        entityManagerFactoryRef = "secondaryEntityManagerFactory",
        transactionManagerRef = "secondaryTransactionManager"
)
public class SecondaryDataSourceConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private HibernatePropertiesConfig hibernatePropertiesConfig;

    @PostConstruct
    public void init() {
        CustomPhysicalNamingStrategy.setApplicationContext(applicationContext);
    }

    @Bean(name = "secondaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "secondaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean secondaryEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("secondaryDataSource") DataSource dataSource) {

        HibernatePropertiesConfig.HibernateConfig config =
            hibernatePropertiesConfig.getSecondary().getHibernate();

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", config.getDdlAuto());
        properties.put("hibernate.dialect", config.getDialect());
        properties.put("hibernate.show_sql", String.valueOf(config.isShowSql()));
        properties.put("hibernate.format_sql", String.valueOf(config.isFormatSql()));
        properties.put("hibernate.physical_naming_strategy",
                      "kr.co.ultari.at_board.config.CustomPhysicalNamingStrategy");

        return builder
                .dataSource(dataSource)
                .packages("kr.co.ultari.at_board.model.secondary")
                .persistenceUnit("secondary")
                .properties(properties)
                .build();
    }

    @Bean(name = "secondaryTransactionManager")
    public PlatformTransactionManager secondaryTransactionManager(
            @Qualifier("secondaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
