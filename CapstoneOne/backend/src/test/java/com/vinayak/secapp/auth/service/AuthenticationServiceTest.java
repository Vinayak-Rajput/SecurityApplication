package com.vinayak.secapp.auth.service;

import com.vinayak.secapp.auth.dto.LoginRequest;
import com.vinayak.secapp.auth.exception.InactiveTokenException;
import com.vinayak.secapp.auth.exception.InvalidCredentialsException;
import com.vinayak.secapp.auth.exception.InvalidTokenException;
import com.vinayak.secapp.logging.AuditEventLogger;
import com.vinayak.secapp.user.entity.User;
import com.vinayak.secapp.user.repository.UserRepository;
import com.vinayak.secapp.common.util.Sha256;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private JwtService jwtService;
    @Mock private TokenService tokenService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditEventLogger auditEventLogger;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(jwtService, tokenService, userRepository, passwordEncoder, auditEventLogger);
    }

    @Test
    void loginRejectsUnknownUsernameWithoutCreatingSession() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("missing", "password"), "192.0.2.10"));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(tokenService, never()).saveSession(any(), any(), any(), any(), any(), any());
    }

    @Test
    void validateTokenReturnsAuthenticatedIdentity() {
        when(jwtService.parseToken("access-token")).thenReturn(tokenDetails(
                "alice", UUID.randomUUID(), "access-id", Instant.now().plusSeconds(60), false));

        var response = authService.validateToken("access-token");

        assertEquals("alice", response.getUsername());
        assertTrue(response.isAuthenticated());
    }

    @Test
    void loginCreatesSessionWithTokenIdAndRefreshHash() {
        User user = user("alice", true, false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct horse", "stored-hash")).thenReturn(true);
        when(jwtService.generateToken(eq("alice"), any(UUID.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(eq("alice"), any(UUID.class))).thenReturn("refresh-token");
        Date accessExpiryDate = Date.from(Instant.now().plusSeconds(60));
        Date refreshExpiryDate = Date.from(Instant.now().plusSeconds(600));
        Instant accessExpiry = accessExpiryDate.toInstant();
        Instant refreshExpiry = refreshExpiryDate.toInstant();
        when(jwtService.parseToken("access-token")).thenReturn(tokenDetails(
                "alice", UUID.randomUUID(), "access-id", accessExpiry, false));
        when(jwtService.parseToken("refresh-token")).thenReturn(tokenDetails(
                "alice", UUID.randomUUID(), "refresh-id", refreshExpiry, true));

        var response = authService.login(new LoginRequest(" alice ", "correct horse"), "192.0.2.10");

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        ArgumentCaptor<UUID> sessionId = ArgumentCaptor.forClass(UUID.class);
        verify(tokenService).saveSession(
                sessionId.capture(), eq("alice"), eq("access-id"),
                eq(Sha256.hash("refresh-token")), eq(accessExpiry), eq(refreshExpiry));
        assertTrue(sessionId.getValue() != null);
        verify(jwtService).generateToken("alice", sessionId.getValue());
        verify(jwtService).generateRefreshToken("alice", sessionId.getValue());
    }

    @Test
    void loginRejectsWrongPasswordWithoutCreatingSession() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user("alice", true, false)));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("alice", "wrong"), "192.0.2.10"));

        verify(tokenService, never()).saveSession(any(), any(), any(), any(), any(), any());
    }

    @Test
    void loginRejectsDisabledOrLockedAccountsAsInvalidCredentials() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user("alice", false, false)));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("alice", "password"), "192.0.2.10"));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(tokenService, never()).saveSession(any(), any(), any(), any(), any(), any());
    }

    @Test
    void loginRejectsLockedAccountWithoutCheckingPassword() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user("alice", true, true)));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("alice", "password"), "192.0.2.10"));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(tokenService, never()).saveSession(any(), any(), any(), any(), any(), any());
    }

    @Test
    void refreshRotatesSessionUsingPresentedTokenHash() {
        UUID sessionId = UUID.randomUUID();
        Date accessExpiryDate = Date.from(Instant.now().plusSeconds(60));
        Date refreshExpiryDate = Date.from(Instant.now().plusSeconds(600));
        Instant accessExpiry = accessExpiryDate.toInstant();
        Instant refreshExpiry = refreshExpiryDate.toInstant();
        when(jwtService.parseToken("old-refresh")).thenReturn(tokenDetails(
                "alice", sessionId, "refresh-id", Instant.now().plusSeconds(600), true));
        when(jwtService.generateToken("alice", sessionId)).thenReturn("new-access");
        when(jwtService.generateRefreshToken("alice", sessionId)).thenReturn("new-refresh");
        when(jwtService.parseToken("new-access")).thenReturn(tokenDetails(
                "alice", sessionId, "new-access-id", accessExpiry, false));
        when(jwtService.parseToken("new-refresh")).thenReturn(tokenDetails(
                "alice", sessionId, "new-refresh-id", refreshExpiry, true));
        when(tokenService.rotateTokens(eq(sessionId), eq(Sha256.hash("old-refresh")), eq("new-access-id"),
                eq(accessExpiry), eq(Sha256.hash("new-refresh")), eq(refreshExpiry))).thenReturn(true);

        var response = authService.refresh("old-refresh");

        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
        verify(tokenService).rotateTokens(sessionId, Sha256.hash("old-refresh"), "new-access-id",
                accessExpiry, Sha256.hash("new-refresh"), refreshExpiry);
    }

    @Test
    void refreshRejectsAccessTokenAndInactiveRefreshSession() {
        when(jwtService.parseToken("access-token")).thenReturn(tokenDetails(
                "alice", UUID.randomUUID(), "access-id", Instant.now().plusSeconds(60), false));
        assertThrows(InvalidTokenException.class,
                () -> authService.refresh("access-token"));
        verify(tokenService, never()).rotateTokens(any(), any(), any(), any(), any(), any());

        UUID sessionId = UUID.randomUUID();
        when(jwtService.parseToken("replayed-refresh")).thenReturn(tokenDetails(
                "alice", sessionId, "refresh-id", Instant.now().plusSeconds(600), true));
        when(jwtService.generateToken("alice", sessionId)).thenReturn("new-access");
        when(jwtService.generateRefreshToken("alice", sessionId)).thenReturn("new-refresh");
        when(jwtService.parseToken("new-access")).thenReturn(tokenDetails(
                "alice", sessionId, "new-access-id", Instant.now().plusSeconds(60), false));
        when(jwtService.parseToken("new-refresh")).thenReturn(tokenDetails(
                "alice", sessionId, "new-refresh-id", Instant.now().plusSeconds(600), true));
        when(tokenService.rotateTokens(any(), any(), any(), any(), any(), any())).thenReturn(false);

        assertThrows(InactiveTokenException.class,
                () -> authService.refresh("replayed-refresh"));
    }

    @Test
    void logoutRevokesSessionUsingCurrentAccessTokenIdentifiers() {
        UUID sessionId = UUID.randomUUID();
        when(jwtService.parseToken("access-token")).thenReturn(tokenDetails(
                "alice", sessionId, "access-id", Instant.now().plusSeconds(60), false));
        when(tokenService.revokeSession(sessionId, "access-id")).thenReturn(true);

        assertEquals("Logged out successfully", authService.logout("access-token").getMessage());

        verify(tokenService).revokeSession(sessionId, "access-id");
    }

    @Test
    void logoutRejectsAlreadyRevokedSession() {
        UUID sessionId = UUID.randomUUID();
        when(jwtService.parseToken("access-token")).thenReturn(tokenDetails(
                "alice", sessionId, "access-id", Instant.now().plusSeconds(60), false));
        when(tokenService.revokeSession(sessionId, "access-id")).thenReturn(false);

        assertThrows(InactiveTokenException.class, () -> authService.logout("access-token"));
    }

    private static User user(String username, boolean enabled, boolean locked) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("stored-hash");
        user.setEnabled(enabled);
        user.setLocked(locked);
        return user;
    }

    private static JwtService.TokenDetails tokenDetails(
            String username,
            UUID sessionId,
            String tokenId,
            Instant expiresAt,
            boolean refreshToken
    ) {
        return new JwtService.TokenDetails(username, sessionId, tokenId, expiresAt, refreshToken);
    }
}
