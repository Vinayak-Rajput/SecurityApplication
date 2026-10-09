package com.hdfclife.capstoneone.common.config;

import com.hdfclife.capstoneone.audit.entity.AuditLogEntry;
import com.hdfclife.capstoneone.common.config.properties.AuditDataSourceProperties;
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
        basePackages = "com.hdfclife.capstoneone.audit.repository",
        entityManagerFactoryRef = "auditLogEntityManagerFactory",
        transactionManagerRef = "auditLogTransactionManager"
)
public class AuditLogDataSourceConfig {

    @Bean(
            name = "auditLogReaderDataSource",
            destroyMethod = "close"
    )
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
                .properties(Map.of(
                        "hibernate.hbm2ddl.auto", "validate",
                        "hibernate.physical_naming_strategy",
                        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
                ))
                .build();
    }

    @Bean(name = "auditLogTransactionManager")
    public JpaTransactionManager auditLogTransactionManager(
            @Qualifier("auditLogEntityManagerFactory") jakarta.persistence.EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
