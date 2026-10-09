package com.hdfclife.capstoneone.common.security;

import com.hdfclife.capstoneone.audit.model.AuditEventType;
import com.hdfclife.capstoneone.common.util.ErrorResponseFactory;
import com.hdfclife.capstoneone.logging.ApplicationLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ApplicationLogger applicationLogger;
    private final ErrorResponseFactory errorResponseFactory;

    @Override
    public void commence(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        applicationLogger.warn(request, AuditEventType.AUTHENTICATION_FAILED, HttpStatus.UNAUTHORIZED.value(),
                exception, "Authentication denied", false);
        errorResponseFactory.write(response, request, HttpStatus.UNAUTHORIZED,
                "Authentication is required to access this resource.");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        applicationLogger.warn(request, AuditEventType.AUTHORIZATION_FAILED, HttpStatus.FORBIDDEN.value(),
                exception, "Access denied", true);
        errorResponseFactory.write(response, request, HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource.");
    }

    public void serviceUnavailable(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception exception
    ) throws IOException {
        applicationLogger.error(request, AuditEventType.DATABASE_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE.value(), exception, "Authentication database unavailable", true);
        errorResponseFactory.write(response, request, HttpStatus.SERVICE_UNAVAILABLE,
                "Authentication data is temporarily unavailable. Please try again later.");
    }
}
