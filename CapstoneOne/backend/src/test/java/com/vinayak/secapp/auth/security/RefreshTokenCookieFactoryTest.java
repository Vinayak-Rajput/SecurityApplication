package com.vinayak.secapp.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenCookieFactoryTest {

    private static final long DEFAULT_LIFETIME_MILLIS = 86_400_000L; // 24 hours
    private RefreshTokenCookieFactory factory;

    @BeforeEach
    void setUp() {
        factory = new RefreshTokenCookieFactory(DEFAULT_LIFETIME_MILLIS, true, "Lax");
    }

    @Nested
    @DisplayName("issue() Tests")
    class IssueTests {

        @Test
        @DisplayName("Should create ResponseCookie with correct value, path, flags, and expiration maxAge")
        void issue_ValidToken_ReturnsConfiguredResponseCookie() {
            String refreshToken = "sample-refresh-token-12345";

            ResponseCookie cookie = factory.issue(refreshToken);

            assertEquals(RefreshTokenCookieFactory.COOKIE_NAME, cookie.getName());
            assertEquals(refreshToken, cookie.getValue());
            assertTrue(cookie.isHttpOnly(), "Cookie must be HttpOnly");
            assertTrue(cookie.isSecure(), "Cookie must be Secure");
            assertEquals("Lax", cookie.getSameSite());
            assertEquals("/refresh", cookie.getPath());
            assertEquals(Duration.ofMillis(DEFAULT_LIFETIME_MILLIS), cookie.getMaxAge());
        }
    }

    @Nested
    @DisplayName("clear() Tests")
    class ClearTests {

        @Test
        @DisplayName("Should create empty ResponseCookie with zero maxAge to clear browser cookie")
        void clear_ReturnsEmptyCookieWithZeroMaxAge() {
            ResponseCookie cookie = factory.clear();

            assertEquals(RefreshTokenCookieFactory.COOKIE_NAME, cookie.getName());
            assertEquals("", cookie.getValue());
            assertTrue(cookie.isHttpOnly());
            assertTrue(cookie.isSecure());
            assertEquals("Lax", cookie.getSameSite());
            assertEquals("/refresh", cookie.getPath());
            assertEquals(Duration.ZERO, cookie.getMaxAge());
        }
    }

    @Nested
    @DisplayName("Configuration & Property Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should respect non-secure configuration flag")
        void issue_InsecureConfig_CreatesCookieWithoutSecureFlag() {
            RefreshTokenCookieFactory customFactory = new RefreshTokenCookieFactory(
                    DEFAULT_LIFETIME_MILLIS, false, "Lax"
            );

            ResponseCookie cookie = customFactory.issue("some-token");

            assertFalse(cookie.isSecure(), "Cookie secure flag should be false when configured false");
        }

        @ParameterizedTest
        @ValueSource(strings = {"Strict", "None", "Lax"})
        @DisplayName("Should set configured SameSite attribute correctly")
        void issue_DifferentSameSiteValues_SetsAttributeCorrectly(String sameSiteValue) {
            RefreshTokenCookieFactory customFactory = new RefreshTokenCookieFactory(
                    DEFAULT_LIFETIME_MILLIS, true, sameSiteValue
            );

            ResponseCookie cookie = customFactory.issue("some-token");

            assertEquals(sameSiteValue, cookie.getSameSite());
        }
    }
}