package com.vinayak.secapp.audit.service;

import com.vinayak.secapp.audit.model.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class AuditLogQueueService {

    private static final Logger STATUS_LOG = LoggerFactory.getLogger("AUDIT_STORAGE_STATUS");

    private final AtomicLong droppedEvents = new AtomicLong();
    private final AtomicLong failedWrites = new AtomicLong();
    private final AuditLogWriterService writerService;

    @Qualifier("auditLogExecutor")
    private final TaskExecutor auditLogExecutor;

    public void enqueue(AuditEvent event) {

        try {
            auditLogExecutor.execute(() -> persist(event));
        }

        catch (RejectedExecutionException exception) { // Queue saturation is observable and never falls back to a synchronous database write on the caller.

            long dropped = droppedEvents.incrementAndGet();

            STATUS_LOG.error(
                    "event=AUDIT_LOG_QUEUE_FULL droppedEvents={} type={}",
                    dropped,
                    event.type()
            );
        }
    }

    private void persist(AuditEvent event) {

        try {
            writerService.persist(event);
        }

        catch (RuntimeException exception) {

            long failures = failedWrites.incrementAndGet();

            STATUS_LOG.error(
                    "event=AUDIT_LOG_WRITE_FAILED failures={} type={} exceptionType={}",
                    failures,
                    event.type(),
                    exception.getClass().getSimpleName()
            );
        }
    }
}
