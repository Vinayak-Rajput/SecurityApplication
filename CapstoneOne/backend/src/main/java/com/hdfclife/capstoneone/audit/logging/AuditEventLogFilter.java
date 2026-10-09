package com.hdfclife.capstoneone.audit.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.hdfclife.capstoneone.audit.model.AuditEventType;

public class AuditEventLogFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        String eventType = event.getMDCPropertyMap().get("eventType");
        return AuditEventType.fromCode(eventType).isPresent() ? FilterReply.NEUTRAL : FilterReply.DENY;
    }
}
