package com.vinayak.secapp.common.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import com.vinayak.secapp.ratelimit.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler(new RateLimitProperties(java.util.Map.of())))
                .build();
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("401 Unauthorized - JwtException maps to AUTHENTICATION_FAILED")
    void handleAuthenticationFailure() throws Exception {
        mockMvc.perform(get("/test/auth-failure"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication failed."))
                .andExpect(jsonPath("$.path").value("/test/auth-failure"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @DisplayName("403 Forbidden - AccessDeniedException sets actorUsername when authenticated")
    void handleAccessDeniedWithAuthenticatedUser() throws Exception {
        // Populate Security Context to test putAuthenticatedActor()
        UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken("vinayak", "password", java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource."));
    }

    @Test
    @DisplayName("400 Bad Request - IllegalArgumentException maps to BAD_REQUEST")
    void handleBadRequest() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request."));
    }

    @Test
    void unexpectedIllegalArgumentIsNotMisreportedAsClientError() throws Exception {
        mockMvc.perform(get("/test/programming-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."));
    }

    @Test
    @DisplayName("409 Conflict - DuplicateUserException returns resource conflict message")
    void handleDuplicateUser() throws Exception {
        mockMvc.perform(get("/test/duplicate-user"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Account could not be created because the username or email is already in use."));
    }

    @Test
    @DisplayName("404 Not Found - UserNotFoundException maps to NOT_FOUND")
    void handleUserNotFound() throws Exception {
        mockMvc.perform(get("/test/user-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("The requested user was not found."));
    }

    @Test
    @DisplayName("429 Too Many Requests - Rate limit error includes Retry-After header")
    void handleRateLimitExceeded() throws Exception {
        mockMvc.perform(get("/test/rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."));
    }

    @Test
    @DisplayName("503 Service Unavailable - CallNotPermittedException (Circuit Breaker)")
    void handleCircuitBreakerOpen() throws Exception {
        mockMvc.perform(get("/test/circuit-open"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Service is temporarily unavailable. Please try again later."));
    }

    @Test
    @DisplayName("503 Service Unavailable - DataAccessResourceFailureException")
    void handleDatabaseUnavailable() throws Exception {
        mockMvc.perform(get("/test/db-unavailable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Authentication data is temporarily unavailable. Please try again later."));
    }

    @Test
    @DisplayName("405 Method Not Allowed - Includes Allow header with supported HTTP methods")
    void handleMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/test/method-not-allowed"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message").value("HTTP method is not supported for this endpoint."));
    }

    @Test
    @DisplayName("500 Internal Server Error - Unhandled generic Exception fallback")
    void handleUnexpectedError() throws Exception {
        mockMvc.perform(get("/test/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."));
    }

    // Dummy Controller to trigger target exceptions
    @RestController
    static class TestController {

        @GetMapping("/test/auth-failure")
        void throwAuthFailure() {
            throw new InvalidCredentialsException("Invalid credentials provided");
        }

        @GetMapping("/test/access-denied")
        void throwAccessDenied() {
            throw new AccessDeniedException("Access denied");
        }

        @GetMapping("/test/bad-request")
        void throwBadRequest() {
            throw new InvalidRequestException("Invalid request");
        }

        @GetMapping("/test/programming-error")
        void throwProgrammingError() {
            throw new IllegalArgumentException("Programming error");
        }

        @GetMapping("/test/duplicate-user")
        void throwDuplicateUser() {
            throw new DuplicateUserException("User already exists");
        }

        @GetMapping("/test/user-not-found")
        void throwUserNotFound() {
            throw new UserNotFoundException("User not found");
        }

        @GetMapping("/test/rate-limit")
        void throwRateLimit() {
            RequestNotPermitted mockException = mock(RequestNotPermitted.class);
            throw mockException;
        }

        @GetMapping("/test/circuit-open")
        void throwCircuitOpen() {
            CallNotPermittedException mockException = mock(CallNotPermittedException.class);
            throw mockException;
        }

        @GetMapping("/test/db-unavailable")
        void throwDbUnavailable() {
            throw new DataAccessResourceFailureException("Database unreachable");
        }

        @GetMapping("/test/method-not-allowed")
        void methodNotAllowedEndpoint() {
            // Handled via POST request to GET endpoint
        }

        @GetMapping("/test/unexpected-error")
        void throwUnexpectedError() {
            throw new NullPointerException("Null reference encountered");
        }
    }

}