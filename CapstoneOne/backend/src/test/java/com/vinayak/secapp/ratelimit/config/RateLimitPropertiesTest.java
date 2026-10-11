package com.vinayak.secapp.ratelimit.config;

import com.vinayak.secapp.ratelimit.config.RateLimitProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RateLimitPropertiesTest {

    @Nested
    @DisplayName("Direct Unit Tests (Record Methods)")
    class RecordLogicTests {

        @Test
        @DisplayName("require() should return LimiterSettings when the requested key exists")
        void requireReturnsSettingsWhenKeyExists() {
            RateLimitProperties.LimiterSettings settings = new RateLimitProperties.LimiterSettings(
                    10,
                    Duration.ofSeconds(1),
                    Duration.ofMillis(500)
            );
            RateLimitProperties properties = new RateLimitProperties(Map.of("auth-service", settings));

            RateLimitProperties.LimiterSettings result = properties.require("auth-service");

            assertNotNull(result);
            assertEquals(10, result.limitForPeriod());
            assertEquals(Duration.ofSeconds(1), result.limitRefreshPeriod());
            assertEquals(Duration.ofMillis(500), result.timeoutDuration());
        }

        @Test
        @DisplayName("require() should throw IllegalStateException when the requested key is missing")
        void requireThrowsExceptionWhenKeyIsMissing() {
            RateLimitProperties properties = new RateLimitProperties(Map.of(
                    "existing-service", new RateLimitProperties.LimiterSettings(5, Duration.ofSeconds(1), Duration.ZERO)
            ));

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> properties.require("missing-service")
            );

            assertEquals("Rate limiter configuration is missing: missing-service", exception.getMessage());
        }

        @Test
        @DisplayName("require() should throw IllegalStateException when instances map is null")
        void requireThrowsExceptionWhenInstancesMapIsNull() {
            RateLimitProperties properties = new RateLimitProperties(null);

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> properties.require("any-service")
            );

            assertEquals("Rate limiter configuration is missing: any-service", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Spring Boot Property Binding Tests")
    class PropertyBindingTests {

        @EnableConfigurationProperties(RateLimitProperties.class)
        static class TestConfig {
        }

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class);

        @Test
        @DisplayName("Spring should bind YAML/Properties to RateLimitProperties and parse Duration strings")
        void bindsPropertiesCorrectly() {
            contextRunner
                    .withPropertyValues(
                            "resilience4j.ratelimiter.instances.login.limit-for-period=20",
                            "resilience4j.ratelimiter.instances.login.limit-refresh-period=1s",
                            "resilience4j.ratelimiter.instances.login.timeout-duration=250ms"
                    )
                    .run(context -> {
                        assertThat(context).hasSingleBean(RateLimitProperties.class);

                        RateLimitProperties props = context.getBean(RateLimitProperties.class);
                        RateLimitProperties.LimiterSettings loginSettings = props.require("login");

                        assertThat(loginSettings.limitForPeriod()).isEqualTo(20);
                        assertThat(loginSettings.limitRefreshPeriod()).isEqualTo(Duration.ofSeconds(1));
                        assertThat(loginSettings.timeoutDuration()).isEqualTo(Duration.ofMillis(250));
                    });
        }

        @Test
        @DisplayName("Spring context should handle multiple configured rate limiter instances")
        void bindsMultipleInstancesCorrectly() {
            contextRunner
                    .withPropertyValues(
                            "resilience4j.ratelimiter.instances.api.limit-for-period=100",
                            "resilience4j.ratelimiter.instances.api.limit-refresh-period=1m",
                            "resilience4j.ratelimiter.instances.api.timeout-duration=0s",
                            "resilience4j.ratelimiter.instances.auth.limit-for-period=5",
                            "resilience4j.ratelimiter.instances.auth.limit-refresh-period=10s",
                            "resilience4j.ratelimiter.instances.auth.timeout-duration=1s"
                    )
                    .run(context -> {
                        RateLimitProperties props = context.getBean(RateLimitProperties.class);

                        assertThat(props.instances()).hasSize(2);
                        assertThat(props.require("api").limitRefreshPeriod()).isEqualTo(Duration.ofMinutes(1));
                        assertThat(props.require("auth").limitForPeriod()).isEqualTo(5);
                    });
        }
    }
}