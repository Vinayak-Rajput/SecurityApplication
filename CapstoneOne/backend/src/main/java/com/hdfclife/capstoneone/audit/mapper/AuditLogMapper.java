package com.hdfclife.capstoneone.audit.mapper;

import com.hdfclife.capstoneone.audit.dto.AuditLogResponse;
import com.hdfclife.capstoneone.audit.entity.AuditLogEntry;
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
