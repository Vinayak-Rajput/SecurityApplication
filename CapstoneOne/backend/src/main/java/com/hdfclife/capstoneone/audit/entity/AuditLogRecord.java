package com.hdfclife.capstoneone.audit.entity;

import com.hdfclife.capstoneone.audit.model.AuditEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.DynamicInsert;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Getter
@Entity
@DynamicInsert
@Table(name = "application_logs", schema = "public")
public class AuditLogRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    protected AuditLogRecord() {
    }

    public static AuditLogRecord from(AuditEvent event) {
        AuditLogRecord record = new AuditLogRecord();
        record.eventTime = OffsetDateTime.now(ZoneOffset.UTC);
        record.level = event.level();
        record.eventType = event.type().name();
        record.actorUsername = event.actorUsername();
        record.subjectUsername = event.subjectUsername();
        record.exceptionType = event.exceptionType();
        record.httpMethod = event.httpMethod();
        record.requestPath = event.requestPath();
        record.httpStatus = event.httpStatus();
        record.durationMs = event.durationMs();
        record.correlationId = event.correlationId();
        return record;
    }
}
