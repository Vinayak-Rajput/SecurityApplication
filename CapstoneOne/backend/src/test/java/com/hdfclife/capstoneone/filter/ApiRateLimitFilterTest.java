package com.hdfclife.capstoneone.filter;

import com.hdfclife.capstoneone.service.ApiRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiRateLimitFilterTest {

    @Mock private ApiRateLimitService rateLimitService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private ApiRateLimitFilter filter;
    private StringWriter responseBody;

    @BeforeEach
    void setUp() {
        filter = new ApiRateLimitFilter(rateLimitService, new ObjectMapper());
        responseBody = new StringWriter();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousRequestsBypassAuthenticatedLimiter() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService, never()).tryAcquire(anyString(), anyString(), anyBoolean());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authenticatedRequestIsAllowedWhenLimiterPermits() throws Exception {
        authenticate("alice");
        when(request.getRequestURI()).thenReturn("/auth");
        when(request.getRemoteAddr()).thenReturn("192.0.2.10");
        when(rateLimitService.tryAcquire("alice", "192.0.2.10", false)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authenticatedRequestIsRejectedWith429WhenLimiterDenies() throws Exception {
        authenticate("alice");
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
        when(request.getRequestURI()).thenReturn("/users");
        when(request.getRemoteAddr()).thenReturn("192.0.2.10");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        when(rateLimitService.tryAcquire("alice", "192.0.2.10", true)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "60");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(responseBody.toString().contains("Too many requests"));
    }

    @Test
    void sensitiveRoutesAreDetectedForUsersAndAuditPaths() throws Exception {
        authenticate("admin");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.tryAcquire(eq("admin"), eq("127.0.0.1"), anyBoolean())).thenReturn(true);

        when(request.getRequestURI()).thenReturn("/users/1");
        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<Boolean> sensitive = ArgumentCaptor.forClass(Boolean.class);
        verify(rateLimitService).tryAcquire(eq("admin"), eq("127.0.0.1"), sensitive.capture());
        assertEquals(true, sensitive.getValue());
    }

    private static void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
