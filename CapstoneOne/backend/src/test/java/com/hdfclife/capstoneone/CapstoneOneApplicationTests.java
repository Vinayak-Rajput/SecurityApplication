package com.hdfclife.capstoneone;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CapstoneOneApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier("auditLogReaderDataSource")
    private DataSource auditLogReaderDataSource;

    @Autowired
    @Qualifier("auditLogJdbcTemplate")
    private JdbcTemplate auditLogJdbcTemplate;

    @Test
    void contextLoadsWithPrimaryAndAuditDatasources() throws Exception {
        assertNotNull(dataSource);
        assertNotNull(auditLogReaderDataSource);
        assertNotNull(auditLogJdbcTemplate);
        assertTrue(dataSource.getConnection().isValid(1));
        assertTrue(auditLogReaderDataSource.getConnection().isValid(1));
    }
}
