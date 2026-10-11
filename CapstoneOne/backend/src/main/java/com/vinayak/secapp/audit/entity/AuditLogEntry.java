package com.vinayak.secapp.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

@Getter
@Entity
@Immutable
@Table(name = "application_logs", schema = "public")
public class AuditLogEntry {

    @Id
    private long id;

    @Column(name = "event_time", nullable = false)
    private OffsetDateTime eventTime;

    @Column(nullable = false)
    private String level;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "actor_username")
    private String actorUsername;

    @Column(name = "subject_username")
    private String subjectUsername;

    @Column(name = "exception_type")
    private String exceptionType;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "request_path")
    private String requestPath;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "correlation_id")
    private String correlationId;

    protected AuditLogEntry() {
    }
}
