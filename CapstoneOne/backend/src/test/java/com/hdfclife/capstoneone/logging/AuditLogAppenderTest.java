package com.hdfclife.capstoneone.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogAppenderTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockStatement;

    @Mock
    private ILoggingEvent mockEvent;

    @Mock
    private IThrowableProxy mockThrowableProxy;

    private MockedStatic<DriverManager> mockedDriverManager;
    private AuditLogAppender appender;

    @BeforeEach
    void setUp() throws SQLException {
        mockedDriverManager = mockStatic(DriverManager.class);
        mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
                .thenReturn(mockConnection);

        lenient().when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        appender = new AuditLogAppender();
        appender.setJdbcUrl("jdbc:postgresql://localhost:5432/audit_db");
        appender.setUsername("audit_user");
        appender.setPassword("secret");
    }

    @AfterEach
    void tearDown() {
        if (appender.isStarted()) {
            appender.stop();
        }
        mockedDriverManager.close();
    }

    @Test
    @DisplayName("start() should initialize JDBC connection and mark appender as started")
    void startSuccessfullyEstablishesConnection() throws SQLException {
        appender.start();

        assertTrue(appender.isStarted());
        mockedDriverManager.verify(() -> DriverManager.getConnection(
                eq("jdbc:postgresql://localhost:5432/audit_db"),
                argThat(props -> "audit_user".equals(props.getProperty("user")) &&
                                 "secret".equals(props.getProperty("password")) &&
                                 "3".equals(props.getProperty("connectTimeout")))
        ));
    }

    @Test
    @DisplayName("append() should map all MDC fields and execute SQL insert successfully")
    void appendSuccessfullyMapsFieldsAndExecutesInsert() throws SQLException {
        appender.start();

        long timestamp = 1700000000000L;
        when(mockEvent.getTimeStamp()).thenReturn(timestamp);
        when(mockEvent.getLevel()).thenReturn(Level.INFO);
        when(mockEvent.getMDCPropertyMap()).thenReturn(Map.of(
                "eventType", "USER_LOGIN",
                "actorUsername", "alice",
                "subjectUsername", "bob",
                "httpMethod", "POST",
                "requestPath", "/api/v1/login",
                "httpStatus", "200",
                "durationMs", "145",
                "correlationId", "corr-1234"
        ));
        when(mockEvent.getThrowableProxy()).thenReturn(mockThrowableProxy);
        when(mockThrowableProxy.getClassName()).thenReturn("java.lang.IllegalArgumentException");

        appender.doAppend(mockEvent);

        verify(mockStatement).setObject(eq(1), eq(java.time.Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC)));
        verify(mockStatement).setString(2, "INFO");
        verify(mockStatement).setString(3, "USER_LOGIN");
        verify(mockStatement).setString(4, "alice");
        verify(mockStatement).setString(5, "bob");
        verify(mockStatement).setString(6, "java.lang.IllegalArgumentException");
        verify(mockStatement).setString(7, "POST");
        verify(mockStatement).setString(8, "/api/v1/login");
        verify(mockStatement).setObject(9, 200);
        verify(mockStatement).setObject(10, 145L);
        verify(mockStatement).setString(11, "corr-1234");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("append() should fallback to defaults for missing MDC properties and invalid numbers")
    void appendHandlesMissingMdcAndInvalidNumbersGracefully() throws SQLException {
        appender.start();

        when(mockEvent.getTimeStamp()).thenReturn(1700000000000L);
        when(mockEvent.getLevel()).thenReturn(Level.WARN);
        when(mockEvent.getMDCPropertyMap()).thenReturn(Map.of(
                "httpStatus", "not_a_number",
                "durationMs", "invalid_long"
        ));
        when(mockEvent.getThrowableProxy()).thenReturn(null);

        appender.doAppend(mockEvent);

        verify(mockStatement).setString(3, "LOG_EVENT"); // Default fallback
        verify(mockStatement).setString(4, null);
        verify(mockStatement).setString(5, null);
        verify(mockStatement).setString(6, null); // Throwable proxy is null
        verify(mockStatement).setObject(9, null); // NumberFormatException handled
        verify(mockStatement).setObject(10, null); // NumberFormatException handled
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("SQLException during insert should close connection and throttle immediate retries")
    void sqlExceptionOnInsertClosesConnectionAndEnforcesCooldown() throws SQLException {
        appender.start();

        when(mockEvent.getTimeStamp()).thenReturn(1700000000000L);
        when(mockEvent.getLevel()).thenReturn(Level.ERROR);
        when(mockEvent.getMDCPropertyMap()).thenReturn(Map.of());
        when(mockStatement.executeUpdate()).thenThrow(new SQLException("Database connection lost"));

        // First event causes exception and closes connection
        appender.doAppend(mockEvent);

        verify(mockConnection).close();

        // Second event immediately after should be dropped due to RECONNECT_DELAY_NANOS cooldown
        appender.doAppend(mockEvent);

        // DriverManager.getConnection called only once during start()
        mockedDriverManager.verify(() -> DriverManager.getConnection(anyString(), any(Properties.class)), times(1));
    }

    @Test
    @DisplayName("start() should fail gracefully if initial JDBC connection fails")
    void startHandlesConnectionFailureGracefully() throws SQLException {
        mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
                .thenThrow(new SQLException("Connection refused"));

        assertDoesNotThrow(() -> appender.start());
        assertTrue(appender.isStarted()); // AppenderBase marks started even if init fails
    }

    @Test
    @DisplayName("stop() should close active connection cleanly")
    void stopClosesActiveConnection() throws SQLException {
        appender.start();
        appender.stop();

        assertFalse(appender.isStarted());
        verify(mockConnection).close();
    }
}