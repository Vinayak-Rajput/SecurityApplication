package com.vinayak.secapp.ratelimit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(Map<String, LimiterSettings> instances, Duration retryAfter) {

    @ConstructorBinding
    public RateLimitProperties {
        retryAfter = retryAfter == null ? Duration.ofSeconds(60) : retryAfter;
    }

    public RateLimitProperties(Map<String, LimiterSettings> instances) {
        this(instances, Duration.ofSeconds(60));
    }

    public record LimiterSettings(
            int limitForPeriod,
            Duration limitRefreshPeriod,
            Duration timeoutDuration
    ) {
    }
}
