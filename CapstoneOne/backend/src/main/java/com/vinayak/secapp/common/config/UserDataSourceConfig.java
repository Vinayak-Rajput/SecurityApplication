package com.vinayak.secapp.common.config;

import com.vinayak.secapp.auth.entity.Authentication;
import com.vinayak.secapp.common.config.properties.MainDataSourceProperties;
import com.vinayak.secapp.user.entity.User;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
        basePackages = {"com.vinayak.secapp.auth.repository", "com.vinayak.secapp.user.repository"},
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class UserDataSourceConfig {

    @Bean(name = "dataSource", destroyMethod = "close")
    @Primary
    public HikariDataSource dataSource(MainDataSourceProperties properties) {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(properties.url())
                .username(properties.username())
                .password(properties.password())
                .build();
    }

    @Bean(name = "entityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dataSource") DataSource dataSource
    ) {
        return builder
                .dataSource(dataSource)
                .packages(User.class, Authentication.class)
                .persistenceUnit("application")
                .properties(Map.of(
                        "hibernate.hbm2ddl.auto", "validate",
                        "hibernate.physical_naming_strategy",
                        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
                ))
                .build();
    }

    @Bean(name = "transactionManager")
    @Primary
    public JpaTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") jakarta.persistence.EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
