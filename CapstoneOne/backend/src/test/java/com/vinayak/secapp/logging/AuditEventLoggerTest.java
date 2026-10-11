package com.vinayak.secapp.logging;

import com.vinayak.secapp.audit.model.AuditEvent;
import com.vinayak.secapp.audit.model.AuditEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditEventLoggerTest {

    private final AuditEventLogger auditEventLogger = new AuditEventLogger();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void restoresPreviousContextAfterWritingAuditEvent() {
        MDC.put("correlationId", "request-id");
        MDC.put("eventType", "REQUEST_STARTED");

        auditEventLogger.log(AuditEvent.request(
                AuditEventType.USER_SELF_VIEWED,
                "alice",
                "alice",
                "GET",
                "/users/me",
                200,
                "User viewed own account"
        ));

        assertEquals("request-id", MDC.get("correlationId"));
        assertEquals("REQUEST_STARTED", MDC.get("eventType"));
        assertNull(MDC.get("actorUsername"));
    }
}
