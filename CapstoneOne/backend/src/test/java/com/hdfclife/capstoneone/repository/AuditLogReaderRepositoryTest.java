package com.hdfclife.capstoneone.repository;

import com.hdfclife.capstoneone.audit.dto.AuditLogResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogReaderRepositoryTest {

    private static final String SELECT_LOGS_SQL = "SELECT * FROM application_logs LIMIT ? OFFSET ?";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository = new AuditLogRepository(
                jdbcTemplate,
                new ByteArrayResource(SELECT_LOGS_SQL.getBytes())
        );
    }

    @Test
    @DisplayName("findRecentLogs should map and return logs when query returns rows")
    void findRecentLogsRunsAuditQueryAndMapsRows() throws Exception {
        OffsetDateTime eventTime = OffsetDateTime.parse("2026-09-27T10:00:00Z");
        String correlationId = UUID.randomUUID().toString();

        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("event_time", OffsetDateTime.class)).thenReturn(eventTime);
        when(resultSet.getString("level")).thenReturn("INFO");
        when(resultSet.getString("event_type")).thenReturn("LOGIN_SUCCESS");
        when(resultSet.getString("actor_username")).thenReturn("alice");
        when(resultSet.getString("subject_username")).thenReturn(null);
        when(resultSet.getString("exception_type")).thenReturn(null);
        when(resultSet.getString("http_method")).thenReturn("POST");
        when(resultSet.getString("request_path")).thenReturn("/login");
        when(resultSet.getObject("http_status", Integer.class)).thenReturn(200);
        when(resultSet.getObject("duration_ms", Long.class)).thenReturn(12L);
        when(resultSet.getString("correlation_id")).thenReturn(correlationId);

        when(jdbcTemplate.query(eq(SELECT_LOGS_SQL), any(RowMapper.class), eq(10), eq(5)))
                .thenAnswer(invocation -> {
                    RowMapper<AuditLogResponse> rowMapper = invocation.getArgument(1);
                    return List.of(rowMapper.mapRow(resultSet, 0));
                });

        List<AuditLogResponse> logs = auditLogRepository.findRecentLogs(10, 5);

        assertEquals(1, logs.size());
        AuditLogResponse log = logs.get(0);
        assertEquals(1L, log.id());
        assertEquals(eventTime, log.eventTime());
        assertEquals("LOGIN_SUCCESS", log.eventType());
        assertEquals("alice", log.actorUsername());
        assertNull(log.subjectUsername());
        assertNull(log.exceptionType());
        assertEquals(200, log.httpStatus());
        assertEquals(12L, log.durationMs());
        assertEquals(correlationId, log.correlationId());

        verify(jdbcTemplate).query(eq(SELECT_LOGS_SQL), any(RowMapper.class), eq(10), eq(5));
    }

    @Test
    @DisplayName("findRecentLogs should return empty list when no records exist")
    void findRecentLogsReturnsEmptyListWhenNoRecordsFound() {
        when(jdbcTemplate.query(eq(SELECT_LOGS_SQL), any(RowMapper.class), eq(10), eq(0)))
                .thenReturn(Collections.emptyList());

        List<AuditLogResponse> logs = auditLogRepository.findRecentLogs(10, 0);

        assertNotNull(logs);
        assertTrue(logs.isEmpty());
        verify(jdbcTemplate).query(eq(SELECT_LOGS_SQL), any(RowMapper.class), eq(10), eq(0));
    }

    @Test
    @DisplayName("findRecentLogs should propagate DataAccessException when database fails")
    void findRecentLogsThrowsExceptionOnDatabaseError() {
        when(jdbcTemplate.query(eq(SELECT_LOGS_SQL), any(RowMapper.class), anyInt(), anyInt()))
                .thenThrow(new QueryTimeoutException("Database connection timed out"));

        assertThrows(DataAccessException.class, () -> auditLogRepository.findRecentLogs(10, 0));
        verify(jdbcTemplate).query(eq(SELECT_LOGS_SQL), any(RowMapper.class), eq(10), eq(0));
    }
}