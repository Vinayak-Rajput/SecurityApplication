package com.hdfclife.capstoneone.service;

import com.hdfclife.capstoneone.auth.entity.AuthSession;
import com.hdfclife.capstoneone.auth.repository.AuthSessionRepository;
import com.hdfclife.capstoneone.common.util.Sha256;
import com.hdfclife.capstoneone.auth.util.TokenFormats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock private AuthSessionRepository sessionRepository;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(sessionRepository);
    }

    @Test
    void saveSessionStoresOnlyRefreshHashAndAccessTokenId() {
        UUID id = UUID.randomUUID();
        Instant accessExpiry = Instant.now().plusSeconds(60);
        Instant refreshExpiry = Instant.now().plusSeconds(600);

        tokenService.saveSession(id, "alice", "access-id", Sha256.hash("refresh"), accessExpiry, refreshExpiry);

        var captor = org.mockito.ArgumentCaptor.forClass(AuthSession.class);
        verify(sessionRepository).save(captor.capture());
        AuthSession saved = captor.getValue();
        assertEquals(id, saved.getId());
        assertEquals("alice", saved.getUsername());
        assertEquals("access-id", saved.getAccessTokenId());
        assertEquals(Sha256.hash("refresh"), saved.getRefreshTokenHash());
        assertFalse(saved.isRevoked());
    }

    @Test
    void refreshReplayRevokesSessionAndDoesNotReplaceStoredTokens() {
        UUID id = UUID.randomUUID();
        AuthSession session = activeSession(id, "current-access", Sha256.hash("current-refresh"));
        when(sessionRepository.findByIdForUpdate(id)).thenReturn(Optional.of(session));

        boolean rotated = tokenService.rotateTokens(id, Sha256.hash("old-refresh"), "new-access",
                Instant.now().plusSeconds(60), Sha256.hash("new-refresh"), Instant.now().plusSeconds(600));

        assertFalse(rotated);
        assertTrue(session.isRevoked());
        assertEquals("current-access", session.getAccessTokenId());
        assertEquals(Sha256.hash("current-refresh"), session.getRefreshTokenHash());
        verify(sessionRepository).save(session);
    }

    @Test
    void refreshWithCurrentCredentialAtomicallyReplacesStoredValues() {
        UUID id = UUID.randomUUID();
        String currentRefreshHash = Sha256.hash("current-refresh");
        AuthSession session = activeSession(id, "current-access", currentRefreshHash);
        when(sessionRepository.findByIdForUpdate(id)).thenReturn(Optional.of(session));
        Instant accessExpiry = Instant.now().plusSeconds(120);
        Instant refreshExpiry = Instant.now().plusSeconds(1200);

        boolean rotated = tokenService.rotateTokens(id, currentRefreshHash, "new-access-id", accessExpiry,
                Sha256.hash("new-refresh"), refreshExpiry);

        assertTrue(rotated);
        assertEquals("new-access-id", session.getAccessTokenId());
        assertEquals(accessExpiry, session.getAccessTokenExpiresAt());
        assertEquals(Sha256.hash("new-refresh"), session.getRefreshTokenHash());
        assertEquals(refreshExpiry, session.getRefreshTokenExpiresAt());
        verify(sessionRepository).save(session);
    }

    @Test
    void expiredRefreshSessionIsNotRotated() {
        UUID id = UUID.randomUUID();
        AuthSession session = activeSession(id, "access", Sha256.hash("refresh"));
        session.setRefreshTokenExpiresAt(Instant.now().minusSeconds(1));
        when(sessionRepository.findByIdForUpdate(id)).thenReturn(Optional.of(session));

        assertFalse(tokenService.rotateTokens(id, Sha256.hash("refresh"), "new-access",
                Instant.now().plusSeconds(60), Sha256.hash("new-refresh"), Instant.now().plusSeconds(600)));

        verify(sessionRepository, never()).save(session);
    }

    @Test
    void accessTokenIsActiveOnlyWhenSessionIsCurrentAndNotExpired() {
        UUID id = UUID.randomUUID();
        AuthSession session = activeSession(id, "current-access", Sha256.hash("refresh"));
        when(sessionRepository.findByIdAndRevokedFalse(id)).thenReturn(Optional.of(session));

        assertTrue(tokenService.isAccessTokenActive(id, "current-access"));
        assertFalse(tokenService.isAccessTokenActive(id, "rotated-access"));

        session.setAccessTokenExpiresAt(Instant.now().minusSeconds(1));
        assertFalse(tokenService.isAccessTokenActive(id, "current-access"));
    }

    @Test
    void logoutCannotRevokeSessionWithStaleAccessTokenId() {
        UUID id = UUID.randomUUID();
        AuthSession session = activeSession(id, "current-access", Sha256.hash("refresh"));
        when(sessionRepository.findByIdForUpdate(id)).thenReturn(Optional.of(session));

        assertFalse(tokenService.revokeSession(id, "old-access"));

        assertFalse(session.isRevoked());
        verify(sessionRepository, never()).save(session);
    }

    @Test
    void accessTokenIdAndRefreshHashFormatValidatorsAreStrict() {
        assertTrue(TokenFormats.isTokenId(UUID.randomUUID().toString()));
        assertFalse(TokenFormats.isTokenId("not-a-uuid"));
        assertTrue(TokenFormats.isSha256(Sha256.hash("refresh")));
        assertFalse(TokenFormats.isSha256("refresh"));
        assertFalse(TokenFormats.isSha256(null));
    }

    private static AuthSession activeSession(UUID id, String accessId, String refreshHash) {
        AuthSession session = new AuthSession();
        session.setId(id);
        session.setUsername("alice");
        session.setAccessTokenId(accessId);
        session.setRefreshTokenHash(refreshHash);
        session.setAccessTokenExpiresAt(Instant.now().plusSeconds(60));
        session.setRefreshTokenExpiresAt(Instant.now().plusSeconds(600));
        session.setRevoked(false);
        return session;
    }
}
