package com.vinayak.secapp.common.exception;

import com.vinayak.secapp.auth.exception.DisallowedRequestOriginException;
import com.vinayak.secapp.auth.exception.InactiveTokenException;
import com.vinayak.secapp.auth.exception.InvalidAuthorizationHeaderException;
import com.vinayak.secapp.auth.exception.InvalidCredentialsException;
import com.vinayak.secapp.auth.exception.InvalidTokenException;
import com.vinayak.secapp.ratelimit.config.RateLimitProperties;
import com.vinayak.secapp.common.dto.ErrorResponse;
import com.vinayak.secapp.common.util.ErrorResponseFactory;
import com.vinayak.secapp.logging.ApplicationLogger;
import com.vinayak.secapp.audit.model.AuditEventType;
import com.vinayak.secapp.user.exception.DuplicateUserException;
import com.vinayak.secapp.user.exception.UserNotFoundException;
import com.vinayak.secapp.user.exception.UserOperationException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final RateLimitProperties rateLimitProperties;
    private final ApplicationLogger applicationLogger;
    private final ErrorResponseFactory errorResponseFactory;

    @ExceptionHandler({
            JwtException.class,
            InvalidTokenException.class,
            InvalidCredentialsException.class,
            InactiveTokenException.class
    })
    public ResponseEntity<ErrorResponse> handleAuthenticationFailure(RuntimeException exception, HttpServletRequest request) {

        applicationLogger.warn(request, AuditEventType.AUTHENTICATION_FAILED, HttpStatus.UNAUTHORIZED.value(),
                exception, "Authentication failed", false);

        return errorResponseFactory.create(HttpStatus.UNAUTHORIZED, "Authentication failed.", request);
    }

    @ExceptionHandler(DisallowedRequestOriginException.class)
    public ResponseEntity<ErrorResponse> handleDisallowedRequestOrigin(DisallowedRequestOriginException exception, HttpServletRequest request) {

        applicationLogger.warn(request, AuditEventType.AUTHORIZATION_FAILED, HttpStatus.FORBIDDEN.value(), exception, "Access denied", false);

        return errorResponseFactory.create(HttpStatus.FORBIDDEN, "The request origin is not allowed.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {

        applicationLogger.warn(request, AuditEventType.AUTHORIZATION_FAILED, HttpStatus.FORBIDDEN.value(), exception, "Access denied", true);

        return errorResponseFactory.create(HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource.", request);
    }

    @ExceptionHandler({
            InvalidAuthorizationHeaderException.class,
            InvalidRequestException.class,
            MissingRequestHeaderException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {

        applicationLogger.warn(request, AuditEventType.BAD_REQUEST, HttpStatus.BAD_REQUEST.value(),
                exception, "Client request failed", false);

        return errorResponseFactory.create(HttpStatus.BAD_REQUEST, "Invalid request.", request);
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUser(DuplicateUserException exception, HttpServletRequest request) {

        applicationLogger.warn(request, AuditEventType.DUPLICATE_USER, HttpStatus.CONFLICT.value(),
                exception, "Duplicate user registration", false);

        return errorResponseFactory.create(HttpStatus.CONFLICT,
                "Account could not be created because the username or email is already in use.", request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException exception, HttpServletRequest request) {

        applicationLogger.warn(request, AuditEventType.USER_NOT_FOUND, HttpStatus.NOT_FOUND.value(),
                exception, "User resource not found", true);

        return errorResponseFactory.create(HttpStatus.NOT_FOUND, "The requested user was not found.", request);
    }

    @ExceptionHandler(UserOperationException.class)
    public ResponseEntity<ErrorResponse> handleUserOperation(UserOperationException exception, HttpServletRequest request) {

        applicationLogger.warn(request, AuditEventType.USER_ADMIN_ACTION_FAILED, HttpStatus.CONFLICT.value(),
                exception, "User administration action rejected", true);

        return errorResponseFactory.create(HttpStatus.CONFLICT,
                "The requested user operation could not be completed.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailure(MethodArgumentNotValidException exception, HttpServletRequest request) {

        applicationLogger.warn(request, AuditEventType.VALIDATION_FAILED, HttpStatus.BAD_REQUEST.value(),
                exception, "Request validation failed", false);

        return errorResponseFactory.create(HttpStatus.BAD_REQUEST, "Request validation failed.", request);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RequestNotPermitted exception, HttpServletRequest request) {

        applicationLogger.warn(request, AuditEventType.RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS.value(),
                exception, "Request rate limit exceeded", true);

        return errorResponseFactory.create(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.",
                request, response -> response.header("Retry-After",
                        Long.toString(rateLimitProperties.retryAfter().toSeconds())));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(CallNotPermittedException exception, HttpServletRequest request) {

        applicationLogger.warn(request, AuditEventType.CIRCUIT_OPEN, HttpStatus.SERVICE_UNAVAILABLE.value(),
                exception, "Circuit breaker opened", true);

        return errorResponseFactory.create(HttpStatus.SERVICE_UNAVAILABLE,
                "Service is temporarily unavailable. Please try again later.", request);
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseUnavailable(DataAccessResourceFailureException exception, HttpServletRequest request) {

        applicationLogger.error(request, AuditEventType.DATABASE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE.value(), exception,
                "Database resource unavailable", true);

        return errorResponseFactory.create(HttpStatus.SERVICE_UNAVAILABLE,
                "Authentication data is temporarily unavailable. Please try again later.", request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleOtherDatabaseError(DataAccessException exception, HttpServletRequest request) {

        applicationLogger.error(request, AuditEventType.DATABASE_OPERATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                exception, "Database operation failed", true);

        return errorResponseFactory.create(HttpStatus.INTERNAL_SERVER_ERROR,
                "The database operation could not be completed.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {

        applicationLogger.warn(request, AuditEventType.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED.value(),
                exception, "HTTP method not allowed", false);

        return errorResponseFactory.create(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not supported for this endpoint.",
                request, response -> {
                    if (exception.getSupportedHttpMethods() != null) {
                        response.allow(exception.getSupportedHttpMethods().toArray(HttpMethod[]::new));
                    }
                });
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(Exception exception, HttpServletRequest request) {

        applicationLogger.error(request, AuditEventType.UNEXPECTED_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                exception, "Unhandled request error", true);

        return errorResponseFactory.create(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", request);
    }
}
