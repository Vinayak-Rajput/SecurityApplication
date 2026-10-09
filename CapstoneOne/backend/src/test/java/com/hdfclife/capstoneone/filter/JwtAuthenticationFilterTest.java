package com.hdfclife.capstoneone.filter;

import com.hdfclife.capstoneone.security.SecurityExceptionHandlers;
import com.hdfclife.capstoneone.auth.exception.InvalidAuthorizationHeaderException;
import com.hdfclife.capstoneone.auth.exception.InvalidTokenException;
import com.hdfclife.capstoneone.service.AccessTokenAuthenticationService;
import com.hdfclife.capstoneone.auth.security.BearerTokenResolver;
import com.hdfclife.capstoneone.auth.service.JwtService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private BearerTokenResolver bearerTokenResolver;
    @Mock private AccessTokenAuthenticationService accessTokenAuthenticationService;
    @Mock private SecurityExceptionHandlers securityExceptionHandlers;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(
                jwtService,
                bearerTokenResolver,
                accessTokenAuthenticationService,
                securityExceptionHandlers);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingAuthorizationHeaderContinuesFilterChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(bearerTokenResolver, never()).resolve(any());
    }

    @Test
    void malformedAuthorizationHeaderIsRejected() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Token abc");
        when(bearerTokenResolver.resolve("Token abc"))
                .thenThrow(new InvalidAuthorizationHeaderException("bad header"));

        filter.doFilterInternal(request, response, filterChain);

        verify(securityExceptionHandlers).commence(eq(request), eq(response), any());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void invalidJwtIsRejected() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad");
        when(bearerTokenResolver.resolve("Bearer bad")).thenReturn("bad");
        when(jwtService.parseToken("bad")).thenThrow(new InvalidTokenException("invalid"));

        filter.doFilterInternal(request, response, filterChain);

        verify(securityExceptionHandlers).commence(eq(request), eq(response), any());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void inactiveAccessTokenIsRejected() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(bearerTokenResolver.resolve("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(tokenDetails(sessionId));
        when(accessTokenAuthenticationService.loadActiveUser(any())).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(securityExceptionHandlers).commence(eq(request), eq(response), any());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void validTokenAuthenticatesAndContinues() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(bearerTokenResolver.resolve("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(tokenDetails(sessionId));
        when(accessTokenAuthenticationService.loadActiveUser(any())).thenReturn(
                User.withUsername("alice").password("x").roles("USER").build());
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(securityExceptionHandlers, never()).commence(any(), any(), any());
    }

    @Test
    void openCircuitBreakerReturnsServiceUnavailable() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(bearerTokenResolver.resolve("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(tokenDetails(sessionId));

        when(accessTokenAuthenticationService.loadActiveUser(any()))
                .thenThrow(CallNotPermittedException.createCallNotPermittedException(
                        io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("authenticationDatabase")));

        filter.doFilterInternal(request, response, filterChain);

        verify(securityExceptionHandlers).serviceUnavailable(eq(request), eq(response), any(CallNotPermittedException.class));
        verify(filterChain, never()).doFilter(request, response);
        verify(accessTokenAuthenticationService, never()).loadActiveUser(any());
    }

    @Test
    void datasourceFailureDuringSessionLookupReturnsServiceUnavailable() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(bearerTokenResolver.resolve("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(tokenDetails(sessionId));
        when(accessTokenAuthenticationService.loadActiveUser(any()))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        filter.doFilterInternal(request, response, filterChain);

        verify(securityExceptionHandlers).serviceUnavailable(eq(request), eq(response), any(DataAccessResourceFailureException.class));
        verify(filterChain, never()).doFilter(request, response);
    }

    private static JwtService.TokenDetails tokenDetails(UUID sessionId) {
        return new JwtService.TokenDetails(
                "alice", sessionId, "access-id", java.time.Instant.now().plusSeconds(60), false);
    }
}
