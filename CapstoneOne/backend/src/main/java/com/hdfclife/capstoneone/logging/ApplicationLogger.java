package com.hdfclife.capstoneone.logging;

import com.hdfclife.capstoneone.audit.model.AuditEvent;
import com.hdfclife.capstoneone.audit.model.AuditEventType;
import com.hdfclife.capstoneone.audit.service.AuditLogQueueService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationLogger {

    private final AuditLogQueueService auditLogQueueService;

    public void log(AuditEvent event) {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();

        try {
            MDC.put("eventType", event.type().name());
            putOrRemove("actorUsername", event.actorUsername());
            putOrRemove("subjectUsername", event.subjectUsername());
            putIfPresent("httpMethod", event.httpMethod());
            putIfPresent("requestPath", event.requestPath());
            putIfPresent("httpStatus", event.httpStatus() == null ? null : event.httpStatus().toString());
            auditLogQueueService.enqueue(withContext(event, "INFO", null));
            log.info(event.message());
        }
        finally {
            restoreContext(previousContext);
        }
    }

    public void warn(
            HttpServletRequest request,
            AuditEventType eventType,
            int status,
            Exception exception,
            String message,
            boolean includeActor
    ) {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        try {
            setFailureContext(request, eventType, status, exception, includeActor);
            auditLogQueueService.enqueue(withContext(
                    new AuditEvent(
                            eventType, actorUsername(includeActor), null, request.getMethod(), request.getRequestURI(),
                            status, message, "WARN", exception.getClass().getSimpleName(), null,
                            RequestCorrelationId.ensure(request)
                    ),
                    "WARN",
                    exception.getClass().getSimpleName()
            ));
            log.warn("{}: method={}, path={}, exceptionType={}",
                    message, request.getMethod(), request.getRequestURI(), exception.getClass().getSimpleName());
        }
        finally {
            restoreContext(previousContext);
        }
    }

    public void error(
            HttpServletRequest request,
            AuditEventType eventType,
            int status,
            Exception exception,
            String message,
            boolean includeActor
    ) {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        try {
            setFailureContext(request, eventType, status, exception, includeActor);
            auditLogQueueService.enqueue(withContext(
                    new AuditEvent(
                            eventType, actorUsername(includeActor), null, request.getMethod(), request.getRequestURI(),
                            status, message, "ERROR", exception.getClass().getSimpleName(), null,
                            RequestCorrelationId.ensure(request)
                    ),
                    "ERROR",
                    exception.getClass().getSimpleName()
            ));
            log.error("{}: method={}, path={}, exceptionType={}",
                    message, request.getMethod(), request.getRequestURI(), exception.getClass().getSimpleName());
        }
        finally {
            restoreContext(previousContext);
        }
    }

    private AuditEvent withContext(AuditEvent event, String level, String exceptionType) {
        return new AuditEvent(
                event.type(),
                firstPresent(event.actorUsername(), MDC.get("actorUsername")),
                firstPresent(event.subjectUsername(), MDC.get("subjectUsername")),
                firstPresent(event.httpMethod(), MDC.get("httpMethod")),
                firstPresent(event.requestPath(), MDC.get("requestPath")),
                event.httpStatus() == null ? integerValue(MDC.get("httpStatus")) : event.httpStatus(),
                event.message(),
                level,
                firstPresent(exceptionType, MDC.get("exceptionType")),
                longValue(MDC.get("durationMs")),
                firstPresent(event.correlationId(), MDC.get("correlationId"))
        );
    }

    private static String actorUsername(boolean includeActor) {
        if (!includeActor) {
            return null;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                ? authentication.getName()
                : null;
    }

    private static String firstPresent(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private static Integer integerValue(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        }
        catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Long longValue(String value) {
        try {
            return value == null ? null : Long.valueOf(value);
        }
        catch (NumberFormatException exception) {
            return null;
        }
    }

    private void setFailureContext(
            HttpServletRequest request,
            AuditEventType eventType,
            int status,
            Exception exception,
            boolean includeActor
    ) {
        MDC.put("httpMethod", request.getMethod());
        MDC.put("requestPath", request.getRequestURI());
        MDC.put("eventType", eventType.name());
        MDC.put("httpStatus", String.valueOf(status));
        MDC.put("exceptionType", exception.getClass().getSimpleName());
        MDC.put("correlationId", RequestCorrelationId.ensure(request));
        MDC.remove("actorUsername");

        if (includeActor) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken)) {
                MDC.put("actorUsername", authentication.getName());
            }
        }
    }

    private void putIfPresent(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }

    private void putOrRemove(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        }
        else {
            MDC.put(key, value);
        }
    }

    private static void restoreContext(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
        }
        else {
            MDC.setContextMap(context);
        }
    }
}
