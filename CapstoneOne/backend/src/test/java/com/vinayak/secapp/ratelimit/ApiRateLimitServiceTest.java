package com.vinayak.secapp.ratelimit;

import com.vinayak.secapp.ratelimit.config.RateLimitProperties;
import com.vinayak.secapp.security.LocalRateLimiterPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.resilience4j.ratelimiter.RateLimiter;

@ExtendWith(MockitoExtension.class)
class ApiRateLimitServiceTest {

    @Mock
    private RateLimitProperties rateLimitProperties;

    @Mock
    private LocalRateLimiterPool limiterPool;

    @Mock
    private RateLimitProperties.LimiterSettings userSettings;

    @Mock
    private RateLimitProperties.LimiterSettings clientIpSettings;

    @Mock
    private RateLimitProperties.LimiterSettings sensitiveSettings;

    // Resilience4j RateLimiter mocks
    @Mock
    private RateLimiter userLimiter;

    @Mock
    private RateLimiter ipLimiter;

    @Mock
    private RateLimiter sensitiveLimiter;

    private ApiRateLimitService service;

    private static final String USERNAME = "john_doe";
    private static final String CLIENT_IP = "192.168.1.100";

    @BeforeEach
    void setUp() {
        when(rateLimitProperties.require("authenticatedUser")).thenReturn(userSettings);
        when(rateLimitProperties.require("authenticatedClientIp")).thenReturn(clientIpSettings);
        when(rateLimitProperties.require("authenticatedSensitive")).thenReturn(sensitiveSettings);

        service = new ApiRateLimitService(rateLimitProperties, limiterPool);
    }

    @Test
    @DisplayName("Should return false and short-circuit when user rate limit is exceeded")
    void tryAcquire_UserRateLimitExceeded_ReturnsFalse() {
        // Arrange
        when(limiterPool.get("authenticated-user", USERNAME, userSettings)).thenReturn(userLimiter);
        when(userLimiter.acquirePermission()).thenReturn(false);

        // Act
        boolean result = service.tryAcquire(USERNAME, CLIENT_IP, true);

        // Assert
        assertFalse(result);
        verify(limiterPool, never()).get(eq("authenticated-ip"), any(), any());
        verify(limiterPool, never()).get(eq("authenticated-sensitive"), any(), any());
    }

    @Test
    @DisplayName("Should return false and short-circuit sensitive check when client IP rate limit is exceeded")
    void tryAcquire_IpRateLimitExceeded_ReturnsFalse() {
        // Arrange
        when(limiterPool.get("authenticated-user", USERNAME, userSettings)).thenReturn(userLimiter);
        when(userLimiter.acquirePermission()).thenReturn(true);

        when(limiterPool.get("authenticated-ip", CLIENT_IP, clientIpSettings)).thenReturn(ipLimiter);
        when(ipLimiter.acquirePermission()).thenReturn(false);

        // Act
        boolean result = service.tryAcquire(USERNAME, CLIENT_IP, true);

        // Assert
        assertFalse(result);
        verify(limiterPool, never()).get(eq("authenticated-sensitive"), any(), any());
    }

    @Test
    @DisplayName("Should return true for non-sensitive route when user and IP limits pass")
    void tryAcquire_NonSensitiveRoute_AllLimitsPass_ReturnsTrue() {
        // Arrange
        when(limiterPool.get("authenticated-user", USERNAME, userSettings)).thenReturn(userLimiter);
        when(userLimiter.acquirePermission()).thenReturn(true);

        when(limiterPool.get("authenticated-ip", CLIENT_IP, clientIpSettings)).thenReturn(ipLimiter);
        when(ipLimiter.acquirePermission()).thenReturn(true);

        // Act
        boolean result = service.tryAcquire(USERNAME, CLIENT_IP, false);

        // Assert
        assertTrue(result);
        verify(limiterPool, never()).get(eq("authenticated-sensitive"), any(), any());
    }

    @Test
    @DisplayName("Should return false when sensitive route limit is exceeded")
    void tryAcquire_SensitiveRouteExceeded_ReturnsFalse() {
        // Arrange
        when(limiterPool.get("authenticated-user", USERNAME, userSettings)).thenReturn(userLimiter);
        when(userLimiter.acquirePermission()).thenReturn(true);

        when(limiterPool.get("authenticated-ip", CLIENT_IP, clientIpSettings)).thenReturn(ipLimiter);
        when(ipLimiter.acquirePermission()).thenReturn(true);

        when(limiterPool.get("authenticated-sensitive", USERNAME, sensitiveSettings)).thenReturn(sensitiveLimiter);
        when(sensitiveLimiter.acquirePermission()).thenReturn(false);

        // Act
        boolean result = service.tryAcquire(USERNAME, CLIENT_IP, true);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return true when all limits pass for a sensitive route")
    void tryAcquire_SensitiveRouteAllPass_ReturnsTrue() {
        // Arrange
        when(limiterPool.get("authenticated-user", USERNAME, userSettings)).thenReturn(userLimiter);
        when(userLimiter.acquirePermission()).thenReturn(true);

        when(limiterPool.get("authenticated-ip", CLIENT_IP, clientIpSettings)).thenReturn(ipLimiter);
        when(ipLimiter.acquirePermission()).thenReturn(true);

        when(limiterPool.get("authenticated-sensitive", USERNAME, sensitiveSettings)).thenReturn(sensitiveLimiter);
        when(sensitiveLimiter.acquirePermission()).thenReturn(true);

        // Act
        boolean result = service.tryAcquire(USERNAME, CLIENT_IP, true);

        // Assert
        assertTrue(result);
    }
}