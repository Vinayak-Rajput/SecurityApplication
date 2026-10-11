package com.vinayak.secapp.audit.mapper;

import com.vinayak.secapp.audit.dto.AuditLogResponse;
import com.vinayak.secapp.audit.entity.AuditLogEntry;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLogEntry entry) {
        return new AuditLogResponse(
                entry.getId(),
                entry.getEventTime(),
                entry.getLevel(),
                entry.getEventType(),
                entry.getActorUsername(),
                entry.getSubjectUsername(),
                entry.getExceptionType(),
                entry.getHttpMethod(),
                entry.getRequestPath(),
                entry.getHttpStatus(),
                entry.getDurationMs(),
                entry.getCorrelationId()
        );
    }
}
