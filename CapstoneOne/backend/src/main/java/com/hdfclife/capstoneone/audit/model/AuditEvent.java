package com.hdfclife.capstoneone.audit.model;

public record AuditEvent(
        AuditEventType type,
        String actorUsername,
        String subjectUsername,
        String httpMethod,
        String requestPath,
        Integer httpStatus,
        String message,
        String level,
        String exceptionType,
        Long durationMs,
        String correlationId
) {

    public static AuditEvent authentication(AuditEventType type, String actorUsername, String message) {
        return new AuditEvent(type, actorUsername, null, null, null, null, message, "INFO", null, null, null);
    }

    public static AuditEvent request(
            AuditEventType type,
            String actorUsername,
            String subjectUsername,
            String httpMethod,
            String requestPath,
            int httpStatus,
            String message
    ) {
        return new AuditEvent(
                type, actorUsername, subjectUsername, httpMethod, requestPath, httpStatus, message,
                "INFO", null, null, null
        );
    }

}
