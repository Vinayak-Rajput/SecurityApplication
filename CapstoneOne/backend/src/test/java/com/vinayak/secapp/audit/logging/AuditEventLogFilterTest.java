package com.vinayak.secapp.audit.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import com.vinayak.secapp.audit.model.AuditEventType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditEventLogFilterTest {

    private final AuditEventLogFilter filter = new AuditEventLogFilter();

    @Test
    void acceptsKnownPersistedAuditEventTypes() {
        ILoggingEvent event = event(AuditEventType.USER_CREATED.name());

        assertEquals(FilterReply.NEUTRAL, filter.decide(event));
    }

    @Test
    void rejectsUnknownEventTypes() {
        ILoggingEvent event = event("NOT_AN_AUDIT_EVENT");

        assertEquals(FilterReply.DENY, filter.decide(event));
    }

    private static ILoggingEvent event(String eventType) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMDCPropertyMap()).thenReturn(Map.of("eventType", eventType));
        return event;
    }
}
