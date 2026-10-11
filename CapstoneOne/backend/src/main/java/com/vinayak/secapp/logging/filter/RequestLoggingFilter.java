package com.vinayak.secapp.logging.filter;

import com.vinayak.secapp.logging.RequestCorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        String correlationId = RequestCorrelationId.attach(request, response);

        MDC.put("correlationId", correlationId);
        MDC.put("httpMethod", request.getMethod());
        MDC.put("requestPath", request.getRequestURI());
        MDC.put("eventType", "REQUEST_STARTED");

        try {
            filterChain.doFilter(request, response);
        }
        finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;

            MDC.put("eventType", "REQUEST_COMPLETED");
            MDC.put("httpStatus", String.valueOf(response.getStatus()));
            MDC.put("durationMs", String.valueOf(durationMs));

            log.info("Outgoing response: method={}, path={}, status={}, durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);

            if (previousMdc == null) {
                MDC.clear();
            }
            else {
                MDC.setContextMap(previousMdc);
            }
        }
    }
}
