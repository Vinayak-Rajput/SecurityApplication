package com.vinayak.secapp.ratelimit;

import com.vinayak.secapp.ratelimit.config.RateLimitProperties;
import com.vinayak.secapp.ratelimit.config.RateLimitProperties.LimiterSettings;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RateLimitService {

    private final LocalRateLimiterPool limiterPool;
    private final LimiterSettings loginAccountSettings;
    private final LimiterSettings loginClientIpSettings;
    private final LimiterSettings authenticatedUserSettings;
    private final LimiterSettings authenticatedClientIpSettings;
    private final LimiterSettings authenticatedSensitiveSettings;
    private final LimiterSettings tokenEndpointClientIpSettings;

    public RateLimitService(RateLimitProperties properties, LocalRateLimiterPool limiterPool) {
        this.limiterPool = limiterPool;
        this.loginAccountSettings = requireSettings(properties, "loginAccount");
        this.loginClientIpSettings = requireSettings(properties, "loginClientIp");
        this.authenticatedUserSettings = requireSettings(properties, "authenticatedUser");
        this.authenticatedClientIpSettings = requireSettings(properties, "authenticatedClientIp");
        this.authenticatedSensitiveSettings = requireSettings(properties, "authenticatedSensitive");
        this.tokenEndpointClientIpSettings = requireSettings(properties, "tokenEndpointClientIp");
    }

    public <T> T limitLogin(String username, String clientIp, CheckedSupplier<T> operation) throws Throwable {
        RateLimiter accountLimiter = limiterPool.get("login-account", username, loginAccountSettings);
        RateLimiter clientIpLimiter = limiterPool.get("login-ip", clientIp, loginClientIpSettings);

        return accountLimiter.executeCheckedSupplier(() -> clientIpLimiter.executeCheckedSupplier(operation));
    }

    public void limitAuthenticatedRequest(String username, String clientIp, boolean sensitiveRoute) {
        acquire(limiterPool.get("authenticated-user", username, authenticatedUserSettings));
        acquire(limiterPool.get("authenticated-ip", clientIp, authenticatedClientIpSettings));
        if (sensitiveRoute) {
            acquire(limiterPool.get("authenticated-sensitive", username, authenticatedSensitiveSettings));
        }
    }

    public void limitTokenEndpoint(String clientIp) {
        acquire(limiterPool.get("token-endpoint-ip", clientIp, tokenEndpointClientIpSettings));
    }

    private static void acquire(RateLimiter limiter) {
        limiter.executeRunnable(() -> { });
    }

    private static LimiterSettings requireSettings(RateLimitProperties properties, String name) {
        Map<String, LimiterSettings> instances = properties.instances();
        LimiterSettings settings = instances == null ? null : instances.get(name);
        if (settings == null) {
            throw new IllegalStateException("Rate limiter configuration is missing: " + name);
        }
        return settings;
    }
}
