package com.hdfclife.capstoneone.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserDataSourceConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(UserDataSourceConfig.class);

    @Test
    @DisplayName("Context should create primary 'dataSource' bean with injected configuration properties")
    void contextLoadsPrimaryDataSourceSuccessfully() {
        contextRunner
                .withPropertyValues(
                        "spring.datasource.url=jdbc:postgresql://localhost:5432/main_db",
                        "spring.datasource.username=main_user",
                        "spring.datasource.password=main_secret"
                )
                .run(context -> {
                    assertThat(context).hasBean("dataSource");

                    // Verify @Primary behavior - resolves directly as DataSource.class
                    assertThat(context).hasSingleBean(DataSource.class);
                    assertThat(context.getBean(DataSource.class)).isSameAs(context.getBean("dataSource"));

                    HikariDataSource dataSource = context.getBean("dataSource", HikariDataSource.class);
                    assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/main_db");
                    assertThat(dataSource.getUsername()).isEqualTo("main_user");
                    assertThat(dataSource.getPassword()).isEqualTo("main_secret");
                });
    }

    @Test
    @DisplayName("Context startup should fail when required database properties are missing")
    void contextFailsWhenPropertiesAreMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasMessageContaining("Could not resolve placeholder 'spring.datasource.url'");
        });
    }

    @Test
    @DisplayName("DataSource connection pool should close cleanly upon application context shutdown")
    void dataSourceClosesOnContextShutdown() {
        contextRunner
                .withPropertyValues(
                        "spring.datasource.url=jdbc:postgresql://localhost:5432/main_db",
                        "spring.datasource.username=main_user",
                        "spring.datasource.password=main_secret"
                )
                .run(context -> {
                    HikariDataSource dataSource = context.getBean("dataSource", HikariDataSource.class);
                    assertThat(dataSource.isClosed()).isFalse();

                    context.close();

                    assertThat(dataSource.isClosed()).isTrue();
                });
    }
}