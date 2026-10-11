package com.vinayak.secapp.auth.service;

import com.vinayak.secapp.auth.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "test-secret-key-that-is-long-enough-for-hmac-sha256");
        ReflectionTestUtils.setField(jwtService, "expiration", 30_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 600_000L);
    }

    @Test
    void generatedAccessTokenRoundTripsIdentityClaims() {
        UUID sessionId = UUID.randomUUID();
        String token = jwtService.generateToken("alice", sessionId);

        assertEquals("alice", jwtService.extractUsername(token));
        assertEquals(sessionId, jwtService.extractSessionId(token));
        assertTrue(jwtService.isTokenValid(token));
        assertTrue(jwtService.isTokenValid(token, "alice"));
        assertFalse(jwtService.isRefreshToken(token));
        assertFalse(jwtService.extractTokenId(token).isBlank());
        assertEquals("alice", jwtService.parseToken(token).username());
        assertEquals(sessionId, jwtService.parseToken(token).sessionId());
    }

    @Test
    void generatedRefreshTokenIsMarkedAsRefreshType() {
        UUID sessionId = UUID.randomUUID();
        String refresh = jwtService.generateRefreshToken("alice", sessionId);

        assertTrue(jwtService.isRefreshToken(refresh));
        assertEquals(sessionId, jwtService.extractSessionId(refresh));
        assertTrue(jwtService.parseToken(refresh).refreshToken());
    }

    @Test
    void blankOrTamperedTokensAreRejected() {
        assertThrows(InvalidTokenException.class, () -> jwtService.extractUsername(" "));
        assertThrows(Exception.class, () -> jwtService.extractUsername("not-a-jwt"));
    }
}
