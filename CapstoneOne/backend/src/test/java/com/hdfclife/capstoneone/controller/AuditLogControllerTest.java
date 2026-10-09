package com.hdfclife.capstoneone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfclife.capstoneone.audit.dto.AuditLogResponse;
import com.hdfclife.capstoneone.audit.model.AuditEvent;
import com.hdfclife.capstoneone.audit.model.AuditEventType;
import com.hdfclife.capstoneone.audit.service.AuditLogReaderService;
import com.hdfclife.capstoneone.logging.AuditEventLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.web.servlet.function.RequestPredicates.contentType;

@WebMvcTest(AuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuditLogReaderService auditLogReaderService;

    @MockBean
    private AuditEventLogger auditEventLogger;

    private AuditLogResponse createSampleAuditLogResponse(long id, String eventType, String actorUsername) {
        return new AuditLogResponse(
                id,
                OffsetDateTime.now(),
                "INFO",
                eventType,
                actorUsername,
                null,
                null,
                "POST",
                "/login",
                200,
                25L,
                "corr-id-123"
        );
    }

    @Nested
    @DisplayName("GET /audit/logs - Retrieval & Audit Event Logging")
    class GetRecentLogsTests {

        @Test
        @WithMockUser(username = "auditor_john", roles = "AUDITOR")
        @DisplayName("Should return logs with default pagination parameters (limit=100, offset=0) and trigger audit log event")
        void getRecentLogsWithDefaultPagination() throws Exception {
            AuditLogResponse record1 = createSampleAuditLogResponse(1L, "USER_LOGIN", "vinayak");
            AuditLogResponse record2 = createSampleAuditLogResponse(2L, "USER_LOGOUT", "vinayak");
            List<AuditLogResponse> expectedLogs = List.of(record1, record2);

            given(auditLogReaderService.findRecentLogs(100, 0)).willReturn(expectedLogs);

            mockMvc.perform(get("/audit/logs")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect((ResultMatcher) contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].eventType").value("USER_LOGIN"))
                    .andExpect(jsonPath("$[0].actorUsername").value("vinayak"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].eventType").value("USER_LOGOUT"));

            verify(auditLogReaderService).findRecentLogs(100, 0);

            ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditEventLogger).log(eventCaptor.capture());

            AuditEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.type()).isEqualTo(AuditEventType.AUDIT_LOG_ACCESS);
            assertThat(capturedEvent.actorUsername()).isEqualTo("auditor_john");
            assertThat(capturedEvent.httpMethod()).isEqualTo("GET");
            assertThat(capturedEvent.requestPath()).isEqualTo("/audit/logs");
            assertThat(capturedEvent.httpStatus()).isEqualTo(200);
            assertThat(capturedEvent.message())
                    .isEqualTo("Auditor accessed audit logs: limit=100, offset=0, returned=2");
        }

        @Test
        @WithMockUser(username = "admin_auditor", roles = "AUDITOR")
        @DisplayName("Should parse custom query parameters for limit and offset correctly")
        void getRecentLogsWithCustomPagination() throws Exception {
            AuditLogResponse record = createSampleAuditLogResponse(5L, "PASSWORD_CHANGE", "user123");
            given(auditLogReaderService.findRecentLogs(25, 50)).willReturn(List.of(record));

            mockMvc.perform(get("/audit/logs")
                            .param("limit", "25")
                            .param("offset", "50")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(5))
                    .andExpect(jsonPath("$[0].eventType").value("PASSWORD_CHANGE"));

            verify(auditLogReaderService).findRecentLogs(25, 50);

            ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditEventLogger).log(eventCaptor.capture());

            AuditEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.actorUsername()).isEqualTo("admin_auditor");
            assertThat(capturedEvent.message())
                    .isEqualTo("Auditor accessed audit logs: limit=25, offset=50, returned=1");
        }

        @Test
        @WithMockUser(username = "auditor_sec", roles = "AUDITOR")
        @DisplayName("Should return 200 OK with an empty array when no audit records are found")
        void getRecentLogsEmptyResult() throws Exception {
            given(auditLogReaderService.findRecentLogs(100, 0)).willReturn(Collections.emptyList());

            mockMvc.perform(get("/audit/logs")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(auditLogReaderService).findRecentLogs(100, 0);

            ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditEventLogger).log(eventCaptor.capture());

            assertThat(eventCaptor.getValue().message())
                    .isEqualTo("Auditor accessed audit logs: limit=100, offset=0, returned=0");
        }
    }
}