package com.vinayak.secapp.audit.config;

import com.vinayak.secapp.audit.entity.AuditLogRecord;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
    basePackages = "com.vinayak.secapp.audit.writer.repository",
    entityManagerFactoryRef = "auditLogWriterEntityManagerFactory",
    transactionManagerRef = "auditLogWriterTransactionManager"
)
public class AuditLogWriterDataSourceConfig {

    @Bean(name = "auditLogWriterDataSource", destroyMethod = "close")
    public HikariDataSource auditLogWriterDataSource(AuditDataSourceProperties properties) {

        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(properties.url())
                .username(properties.writerUsername())
                .password(properties.writerPassword())
                .build();
    }

    @Bean(name = "auditLogWriterEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean auditLogWriterEntityManagerFactory(
        EntityManagerFactoryBuilder builder,
        @Qualifier("auditLogWriterDataSource") DataSource dataSource
    ) {
        return builder
                .dataSource(dataSource)
                .packages(AuditLogRecord.class)
                .persistenceUnit("auditLogWriter")
                .properties(Map.of(
                        "hibernate.hbm2ddl.auto", "validate",
                        "hibernate.physical_naming_strategy",
                        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
                ))
                .build();
    }

    @Bean(name = "auditLogWriterTransactionManager")
    public JpaTransactionManager auditLogWriterTransactionManager(
            @Qualifier("auditLogWriterEntityManagerFactory") jakarta.persistence.EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
