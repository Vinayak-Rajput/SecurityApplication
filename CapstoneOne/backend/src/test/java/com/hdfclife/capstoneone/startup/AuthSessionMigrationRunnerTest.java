package com.hdfclife.capstoneone.startup;

import com.hdfclife.capstoneone.auth.entity.AuthSession;
import com.hdfclife.capstoneone.auth.repository.AuthSessionRepository;
import com.hdfclife.capstoneone.common.util.Sha256;
import com.hdfclife.capstoneone.auth.util.TokenFormats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthSessionMigrationRunnerTest {

    @Mock
    private AuthSessionRepository authSessionRepository;

    private AuthSessionMigrationService migrationService;

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String VALID_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @BeforeEach
    void setUp() {
        migrationService = new AuthSessionMigrationService(authSessionRepository);
    }

    @Test
    @DisplayName("Should migrate legacy sessions, update token formats, revoke them, and flush repository")
    void run_WhenLegacySessionsExist_MigratesAndFlushes() {
        // Arrange
        AuthSession alreadyValidSession = createSession(VALID_UUID, VALID_SHA256, false);
        AuthSession legacySession = createSession("legacy-access-token", "raw-refresh-token", false);

        when(authSessionRepository.findAll(any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(alreadyValidSession, legacySession),
                        PageRequest.of(0, 500, Sort.by("id").ascending()), 2));

        AuthSessionMigrationService.MigrationBatch batch = migrationService.migrateBatch(0);

        // Assert - Already valid session should remain unchanged
        assertEquals(VALID_UUID, alreadyValidSession.getAccessTokenId());
        assertEquals(VALID_SHA256, alreadyValidSession.getRefreshTokenHash());
        assertFalse(alreadyValidSession.isRevoked());

        // Assert - Legacy session should be updated and revoked
        assertTrue(TokenFormats.isTokenId(legacySession.getAccessTokenId()), "Access token ID should be updated to a valid UUID");
        assertEquals(Sha256.hash("raw-refresh-token"), legacySession.getRefreshTokenHash(), "Refresh token should be SHA-256 hashed");
        assertTrue(legacySession.isRevoked(), "Legacy session should be revoked");

        // Verify repository flush called exactly once
        assertEquals(1, batch.migrated());
        assertFalse(batch.hasNext());
        verify(authSessionRepository, times(1)).flush();
        verify(authSessionRepository).findAll(PageRequest.of(0, 500, Sort.by("id").ascending()));
    }

    @Test
    @DisplayName("Should do nothing and not flush repository when all sessions are already in modern format")
    void run_WhenAllSessionsAlreadyValid_DoesNotFlush() {
        // Arrange
        AuthSession validSession1 = createSession(VALID_UUID, VALID_SHA256, false);
        AuthSession validSession2 = createSession(UUID.randomUUID().toString(), VALID_SHA256, false);

        when(authSessionRepository.findAll(any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(validSession1, validSession2),
                        PageRequest.of(0, 500, Sort.by("id").ascending()), 2));

        AuthSessionMigrationService.MigrationBatch batch = migrationService.migrateBatch(0);

        assertEquals(0, batch.migrated());
        verify(authSessionRepository, never()).flush();
    }

    @Test
    @DisplayName("Should handle empty repository gracefully without flushing")
    void run_WhenNoSessionsExist_DoesNotFlush() {
        // Arrange
        when(authSessionRepository.findAll(any(Pageable.class))).thenReturn(
                new PageImpl<AuthSession>(List.of(), PageRequest.of(0, 500, Sort.by("id").ascending()), 0));

        AuthSessionMigrationService.MigrationBatch batch = migrationService.migrateBatch(0);

        assertEquals(0, batch.migrated());
        assertFalse(batch.hasNext());
        verify(authSessionRepository, never()).flush();
    }

    @Test
    void runnerContinuesUntilEveryBatchHasBeenProcessed() {
        AuthSessionMigrationService migrationService = mock(AuthSessionMigrationService.class);
        when(migrationService.migrateBatch(0)).thenReturn(new AuthSessionMigrationService.MigrationBatch(2, true));
        when(migrationService.migrateBatch(1)).thenReturn(new AuthSessionMigrationService.MigrationBatch(1, false));

        new AuthSessionMigrationRunner(migrationService).run(mock(ApplicationArguments.class));

        verify(migrationService).migrateBatch(0);
        verify(migrationService).migrateBatch(1);
        verifyNoMoreInteractions(migrationService);
    }

    // Helper method to instantiate AuthSession for testing
    private AuthSession createSession(String accessTokenId, String refreshTokenHash, boolean revoked) {
        AuthSession session = new AuthSession();
        session.setAccessTokenId(accessTokenId);
        session.setRefreshTokenHash(refreshTokenHash);
        session.setRevoked(revoked);
        return session;
    }
}