package com.hdfclife.capstoneone.service;

import com.hdfclife.capstoneone.common.config.properties.RateLimitProperties;
import com.hdfclife.capstoneone.security.LocalRateLimiterPool;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    @Test
    void loginLimiterNormalizesAccountNamesAndKeepsAccountLimitsIndependent() {
        RateLimitProperties properties = new RateLimitProperties(Map.of(
                "loginAccount", settings(2),
                "loginClientIp", settings(10)
        ));
        LoginRateLimitService limits = new LoginRateLimitService(properties, new LocalRateLimiterPool());

        assertTrue(limits.execute(" Alice ", "192.0.2.1", () -> true));
        assertTrue(limits.execute("alice", "192.0.2.1", () -> true));
        assertThrows(RequestNotPermitted.class, () -> limits.execute("ALICE", "192.0.2.1", () -> true));
        assertTrue(limits.execute("bob", "192.0.2.1", () -> true));
    }

    @Test
    void authenticatedLimiterAppliesUserIpAndSensitiveRouteLimits() {
        RateLimitProperties properties = new RateLimitProperties(Map.of(
                "authenticatedUser", settings(3),
                "authenticatedClientIp", settings(10),
                "authenticatedSensitive", settings(1)
        ));
        ApiRateLimitService limits =
                new ApiRateLimitService(properties, new LocalRateLimiterPool());

        assertTrue(limits.tryAcquire("alice", "192.0.2.1", true));
        assertFalse(limits.tryAcquire("alice", "192.0.2.1", true));
        assertTrue(limits.tryAcquire("alice", "192.0.2.1", false));
        assertTrue(limits.tryAcquire("bob", "192.0.2.1", true));
        assertFalse(limits.tryAcquire("alice", "192.0.2.1", false));
    }

    private static RateLimitProperties.LimiterSettings settings(int limit) {
        return new RateLimitProperties.LimiterSettings(limit, Duration.ofHours(1), Duration.ZERO);
    }
}
