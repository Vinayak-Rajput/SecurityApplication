package com.vinayak.secapp.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinayak.secapp.audit.model.AuditEventType;
import com.vinayak.secapp.logging.RequestCorrelationId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class SecurityExceptionHandlersTest {

    private SecurityExceptionHandlers exceptionHandlers;
    private ObjectMapper objectMapper;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private MockedStatic<RequestCorrelationId> requestCorrelationIdMock;

    private static final String CORRELATION_ID = "test-correlation-id-123";
    private static final String REQUEST_URI = "/api/v1/resource";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        exceptionHandlers = new SecurityExceptionHandlers(objectMapper);

        request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI(REQUEST_URI);

        response = new MockHttpServletResponse();

        // Mock static RequestCorrelationId utility methods
        requestCorrelationIdMock = mockStatic(RequestCorrelationId.class);
        requestCorrelationIdMock.when(() -> RequestCorrelationId.ensure(any())).thenReturn(CORRELATION_ID);
        requestCorrelationIdMock.when(() -> RequestCorrelationId.attach(any(), any())).thenReturn(CORRELATION_ID);
    }

    @AfterEach
    void tearDown() {
        requestCorrelationIdMock.close();
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Nested
    @DisplayName("commence() Tests - AuthenticationEntryPoint")
    class CommenceTests {

        @Test
        @DisplayName("Should write 401 Unauthorized JSON error response and populate MDC context")
        void commence_AuthenticationException_Writes401ResponseAndPopulatesMdc() throws IOException {
            BadCredentialsException exception = new BadCredentialsException("Bad credentials");

            exceptionHandlers.commence(request, response, exception);

            // Assert HTTP Response
            assertEquals(401, response.getStatus());
            assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());

            // Assert JSON Body
            String jsonOutput = response.getContentAsString();
            assertTrue(jsonOutput.contains("\"status\":401"));
            assertTrue(jsonOutput.contains("\"error\":\"Unauthorized\""));
            assertTrue(jsonOutput.contains("\"message\":\"Authentication is required to access this resource.\""));
            assertTrue(jsonOutput.contains("\"path\":\"" + REQUEST_URI + "\""));
            assertTrue(jsonOutput.contains("\"correlationId\":\"" + CORRELATION_ID + "\""));

            // Assert MDC Context
            assertEquals("POST", MDC.get("httpMethod"));
            assertEquals(REQUEST_URI, MDC.get("requestPath"));
            assertEquals(AuditEventType.AUTHENTICATION_FAILED.name(), MDC.get("eventType"));
            assertEquals("401", MDC.get("httpStatus"));
            assertEquals("BadCredentialsException", MDC.get("exceptionType"));
            assertEquals(CORRELATION_ID, MDC.get("correlationId"));
        }
    }

    @Nested
    @DisplayName("handle() Tests - AccessDeniedHandler")
    class HandleTests {

        @Test
        @DisplayName("Should write 403 Forbidden response and capture actorUsername in MDC for authenticated users")
        void handle_AuthenticatedUser_Writes403ResponseAndCapturesActor() throws IOException {
            // Arrange authenticated user in SecurityContext
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "john_doe", "password", AuthorityUtils.createAuthorityList("ROLE_USER")
            );
            SecurityContextHolder.setContext(new SecurityContextImpl(auth));

            AccessDeniedException exception = new AccessDeniedException("Access denied");

            // Act
            exceptionHandlers.handle(request, response, exception);

            // Assert Response
            assertEquals(403, response.getStatus());
            assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());

            String jsonOutput = response.getContentAsString();
            assertTrue(jsonOutput.contains("\"status\":403"));
            assertTrue(jsonOutput.contains("\"error\":\"Forbidden\""));
            assertTrue(jsonOutput.contains("\"message\":\"You do not have permission to access this resource.\""));

            // Assert MDC
            assertEquals("john_doe", MDC.get("actorUsername"));
            assertEquals(AuditEventType.AUTHORIZATION_FAILED.name(), MDC.get("eventType"));
            assertEquals("403", MDC.get("httpStatus"));
        }

        @Test
        @DisplayName("Should not set actorUsername in MDC when user is Anonymous")
        void handle_AnonymousUser_DoesNotSetActorUsernameInMdc() throws IOException {
            // Arrange anonymous authentication in SecurityContext
            AnonymousAuthenticationToken anonAuth = new AnonymousAuthenticationToken(
                    "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
            );
            SecurityContextHolder.setContext(new SecurityContextImpl(anonAuth));

            AccessDeniedException exception = new AccessDeniedException("Access denied");

            // Act
            exceptionHandlers.handle(request, response, exception);

            // Assert
            assertEquals(403, response.getStatus());
            assertNull(MDC.get("actorUsername"));
        }
    }

    @Nested
    @DisplayName("serviceUnavailable() Tests")
    class ServiceUnavailableTests {

        @Test
        @DisplayName("Should write 503 Service Unavailable response and capture actorUsername if authenticated")
        void serviceUnavailable_DatabaseUnavailable_Writes503Response() throws IOException {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "admin_user", "password", AuthorityUtils.createAuthorityList("ROLE_ADMIN")
            );
            SecurityContextHolder.setContext(new SecurityContextImpl(auth));

            Exception dbException = new RuntimeException("Database timeout");

            // Act
            exceptionHandlers.serviceUnavailable(request, response, dbException);

            // Assert Response
            assertEquals(503, response.getStatus());
            assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());

            String jsonOutput = response.getContentAsString();
            assertTrue(jsonOutput.contains("\"status\":503"));
            assertTrue(jsonOutput.contains("\"error\":\"Service Unavailable\""));
            assertTrue(jsonOutput.contains("\"message\":\"Authentication data is temporarily unavailable. Please try again later.\""));

            // Assert MDC
            assertEquals("admin_user", MDC.get("actorUsername"));
            assertEquals(AuditEventType.DATABASE_UNAVAILABLE.name(), MDC.get("eventType"));
            assertEquals("503", MDC.get("httpStatus"));
            assertEquals("RuntimeException", MDC.get("exceptionType"));
        }
    }
}