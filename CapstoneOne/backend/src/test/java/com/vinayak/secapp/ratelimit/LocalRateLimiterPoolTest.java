package com.vinayak.secapp.ratelimit;

import com.vinayak.secapp.ratelimit.config.RateLimitProperties;
import com.vinayak.secapp.common.util.Sha256;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalRateLimiterPoolTest {

    private LocalRateLimiterPool limiterPool;
    private RateLimitProperties.LimiterSettings defaultSettings;

    @BeforeEach
    void setUp() {
        limiterPool = new LocalRateLimiterPool();

        // Mock LimiterSettings properties
        defaultSettings = mock(RateLimitProperties.LimiterSettings.class);
        when(defaultSettings.limitForPeriod()).thenReturn(10);
        when(defaultSettings.limitRefreshPeriod()).thenReturn(Duration.ofSeconds(1));
        when(defaultSettings.timeoutDuration()).thenReturn(Duration.ofMillis(500));
    }

    @Nested
    @DisplayName("Caching Behavior Tests")
    class CachingTests {

        @Test
        @DisplayName("Should return same RateLimiter instance for identical namespace and key")
        void get_SameNamespaceAndKey_ReturnsCachedInstance() {
            RateLimiter limiter1 = limiterPool.get("user-limiter", "user123", defaultSettings);
            RateLimiter limiter2 = limiterPool.get("user-limiter", "user123", defaultSettings);

            assertSame(limiter1, limiter2, "Second call should return cached instance from Caffeine");
        }

        @Test
        @DisplayName("Should return different RateLimiter instances for different keys or namespaces")
        void get_DifferentKeyOrNamespace_ReturnsDifferentInstances() {
            RateLimiter limiterUser1 = limiterPool.get("user-limiter", "user123", defaultSettings);
            RateLimiter limiterUser2 = limiterPool.get("user-limiter", "user456", defaultSettings);
            RateLimiter limiterIp = limiterPool.get("ip-limiter", "user123", defaultSettings);

            assertNotSame(limiterUser1, limiterUser2);
            assertNotSame(limiterUser1, limiterIp);
        }
    }

    @Nested
    @DisplayName("Key Normalization Tests")
    class KeyNormalizationTests {

        @Test
        @DisplayName("Should normalize key casing and whitespace to reuse same cached instance")
        void get_KeyWithCaseAndWhitespace_NormalizesAndSharesCache() {
            RateLimiter limiterStandard = limiterPool.get("auth", "john.doe@example.com", defaultSettings);
            RateLimiter limiterUnnormalized = limiterPool.get("auth", "  John.Doe@EXAMPLE.com  ", defaultSettings);

            assertSame(limiterStandard, limiterUnnormalized, "Keys with whitespace or mixed casing should normalize to identical cache key");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t\n"})
        @DisplayName("Should map null, empty, or blank keys to 'unknown'")
        void get_NullOrBlankKeys_MapsToUnknown(String invalidKey) {
            RateLimiter limiterInvalid = limiterPool.get("auth", invalidKey, defaultSettings);
            RateLimiter limiterExplicitUnknown = limiterPool.get("auth", "unknown", defaultSettings);

            assertSame(limiterExplicitUnknown, limiterInvalid, "Null or blank key should normalize to 'unknown'");
        }
    }

    @Nested
    @DisplayName("Configuration & Hashing Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should build RateLimiter with correct configuration from settings")
        void get_CreatesRateLimiterWithCorrectConfig() {
            RateLimiter limiter = limiterPool.get("test-ns", "key1", defaultSettings);
            RateLimiterConfig config = limiter.getRateLimiterConfig();

            assertNotNull(limiter);
            assertEquals(10, config.getLimitForPeriod());
            assertEquals(Duration.ofSeconds(1), config.getLimitRefreshPeriod());
            assertEquals(Duration.ofMillis(500), config.getTimeoutDuration());
        }

        @Test
        @DisplayName("Should create RateLimiter name containing namespace and SHA-256 hash")
        void get_RateLimiterName_ContainsNamespaceAndHash() {
            String key = "test-user";
            String expectedHash = Sha256.hash(key);

            RateLimiter limiter = limiterPool.get("api", key, defaultSettings);

            assertEquals("api:" + expectedHash, limiter.getName());
        }
    }
}