package com.vinayak.secapp.audit.config;

import com.vinayak.secapp.audit.entity.AuditLogEntry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.vinayak.secapp.audit.repository",
        entityManagerFactoryRef = "auditLogEntityManagerFactory",
        transactionManagerRef = "auditLogTransactionManager"
)
public class AuditLogReaderDataSourceConfig {

    @Bean(name = "auditLogReaderDataSource", destroyMethod = "close")
    public HikariDataSource auditLogReaderDataSource(AuditDataSourceProperties properties) {

        HikariDataSource dataSource = DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .url(properties.url())
            .username(properties.readerUsername())
            .password(properties.readerPassword())
            .build();

        // The audit JPA unit is read-only by configuration as well as by the database role's grants.
        dataSource.setReadOnly(true);

        return dataSource;
    }

    @Bean(name = "auditLogEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean auditLogEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("auditLogReaderDataSource") DataSource dataSource
    ) {
        return builder
            .dataSource(dataSource)
            .packages(AuditLogEntry.class)
            .persistenceUnit("auditLogReader")
            .properties(
                Map.of(
                    "hibernate.hbm2ddl.auto", "validate",
                    "hibernate.physical_naming_strategy",
                    "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
                )
            )
            .build();
    }

    @Bean(name = "auditLogTransactionManager")
    public JpaTransactionManager auditLogTransactionManager(@Qualifier("auditLogEntityManagerFactory") jakarta.persistence.EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }
}
