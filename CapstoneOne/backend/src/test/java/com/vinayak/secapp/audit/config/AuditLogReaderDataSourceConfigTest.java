package com.vinayak.secapp.audit.config;

import com.vinayak.secapp.audit.config.AuditLogReaderDataSourceConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogReaderDataSourceConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AuditLogReaderDataSourceConfig.class);

    @Test
    @DisplayName("Context should initialize auditLogReaderDataSource and auditLogJdbcTemplate beans when properties are provided")
    void contextLoadsBeansSuccessfully() {
        contextRunner
                .withPropertyValues(
                        "audit.datasource.url=jdbc:postgresql://localhost:5432/audit_db",
                        "audit.datasource.reader-username=audit_reader",
                        "audit.datasource.reader-password=reader_pass"
                )
                .run(context -> {
                    assertThat(context).hasBean("auditLogReaderDataSource");
                    assertThat(context).hasBean("auditLogJdbcTemplate");

                    HikariDataSource dataSource = context.getBean("auditLogReaderDataSource", HikariDataSource.class);
                    assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/audit_db");
                    assertThat(dataSource.getUsername()).isEqualTo("audit_reader");
                    assertThat(dataSource.getPassword()).isEqualTo("reader_pass");

                    JdbcTemplate jdbcTemplate = context.getBean("auditLogJdbcTemplate", JdbcTemplate.class);
                    assertThat(jdbcTemplate.getDataSource()).isSameAs(dataSource);
                });
    }

    @Test
    @DisplayName("Context startup should fail when required placeholder properties are missing")
    void contextFailsWhenPropertiesAreMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasMessageContaining("Could not resolve placeholder 'audit.datasource.url'");
        });
    }

    @Test
    @DisplayName("HikariDataSource should be configured with default close destroy method")
    void dataSourceHasCloseDestroyMethod() {
        contextRunner
                .withPropertyValues(
                        "audit.datasource.url=jdbc:postgresql://localhost:5432/audit_db",
                        "audit.datasource.reader-username=audit_reader",
                        "audit.datasource.reader-password=reader_pass"
                )
                .run(context -> {
                    HikariDataSource dataSource = context.getBean("auditLogReaderDataSource", HikariDataSource.class);
                    assertThat(dataSource.isClosed()).isFalse();
                    
                    context.close();
                    
                    assertThat(dataSource.isClosed()).isTrue();
                });
    }
}