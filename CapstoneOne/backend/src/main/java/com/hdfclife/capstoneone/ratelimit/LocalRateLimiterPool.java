package com.hdfclife.capstoneone.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hdfclife.capstoneone.common.config.properties.RateLimitProperties.LimiterSettings;
import com.hdfclife.capstoneone.common.util.Sha256;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

@Component
public class LocalRateLimiterPool {
    private final Cache<String, RateLimiter> limiters = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterAccess(Duration.ofMinutes(15))
            .build();

    public RateLimiter get(String namespace, String key, LimiterSettings settings) {
        String normalizedKey = key == null || key.isBlank() ? "unknown" : key.trim().toLowerCase(Locale.ROOT);
        // Hash identities before using them as cache keys; the bounded cache limits per-key memory growth.
        String limiterName = namespace + ":" + Sha256.hash(normalizedKey);
        return limiters.get(limiterName, ignored -> RateLimiter.of(limiterName, config(settings)));
    }

    private static RateLimiterConfig config(LimiterSettings settings) {
        return RateLimiterConfig.custom()
                .limitForPeriod(settings.limitForPeriod())
                .limitRefreshPeriod(settings.limitRefreshPeriod())
                .timeoutDuration(settings.timeoutDuration())
                .build();
    }
}
