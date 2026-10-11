package com.vinayak.secapp.auth.security;

import com.vinayak.secapp.auth.config.properties.JwtProperties;
import com.vinayak.secapp.auth.config.properties.RefreshCookieProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieFactory {
    public static final String COOKIE_NAME = "refresh_token";
    // Both refresh endpoints live under this path, keeping the cookie off unrelated API requests.
    private static final String COOKIE_PATH = "/refresh";

    private final Duration tokenLifetime;
    private final boolean secure;
    private final String sameSite;

    public RefreshTokenCookieFactory(JwtProperties jwtProperties, RefreshCookieProperties cookieProperties) {
        this.tokenLifetime = jwtProperties.refreshExpiration();
        this.secure = cookieProperties.secure();
        this.sameSite = cookieProperties.sameSite();
    }

    public ResponseCookie issue(String refreshToken) {
        return create(refreshToken, tokenLifetime);
    }

    public ResponseCookie clear() {
        return create("", Duration.ZERO);
    }

    private ResponseCookie create(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
