package com.vinayak.secapp.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "AuditLogResponse", description = "Read-only audit event metadata; does not contain request bodies or credentials")
public record AuditLogResponse(

    @Schema(description = "Unique audit event identifier", example = "42")
    long id,

    @Schema(description = "Time the event was recorded in UTC", example = "2026-09-25T10:15:30Z")
    OffsetDateTime eventTime,

    @Schema(description = "Log severity", example = "INFO")
    String level,

    @Schema(description = "Audit event category", example = "USER_DELETED")
    String eventType,

    @Schema(description = "Username that initiated the action")
    String actorUsername,

    @Schema(description = "Username affected by the action, when applicable")
    String subjectUsername,

    @Schema(description = "Exception class for failed operations, when applicable")
    String exceptionType,

    @Schema(description = "HTTP method associated with the event", example = "DELETE")
    String httpMethod,

    @Schema(description = "Request path associated with the event", example = "/users/42")
    String requestPath,

    @Schema(description = "HTTP response status, when associated with a request", example = "204")
    Integer httpStatus,

    @Schema(description = "Request duration in milliseconds, when available", example = "38")
    Long durationMs,

    @Schema(description = "Request correlation ID for log tracing")
    String correlationId
) { }
