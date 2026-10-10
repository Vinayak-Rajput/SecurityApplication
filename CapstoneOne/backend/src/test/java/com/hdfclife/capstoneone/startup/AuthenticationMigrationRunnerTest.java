package com.hdfclife.capstoneone.startup;

import com.hdfclife.capstoneone.auth.entity.Authentication;
import com.hdfclife.capstoneone.auth.repository.AuthenticationSessionRepository;
import com.hdfclife.capstoneone.common.util.Sha256;
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
class AuthenticationMigrationRunnerTest {

    @Mock
    private AuthenticationSessionRepository authenticationSessionRepository;

    private AuthSessionMigrationService migrationService;

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String VALID_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @BeforeEach
    void setUp() {
        migrationService = new AuthSessionMigrationService(authenticationSessionRepository);
    }

    @Test
    @DisplayName("Should migrate legacy sessions, update token formats, revoke them, and flush repository")
    void run_WhenLegacySessionsExist_MigratesAndFlushes() {
        // Arrange
        Authentication alreadyValidSession = createSession(VALID_UUID, VALID_SHA256, false);
        Authentication legacySession = createSession("legacy-access-token", "raw-refresh-token", false);

        when(authenticationSessionRepository.findAll(any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(alreadyValidSession, legacySession),
                        PageRequest.of(0, 500, Sort.by("id").ascending()), 2));

        AuthSessionMigrationService.MigrationBatch batch = migrationService.migrateBatch(0);

        // Assert - Already valid session should remain unchanged
        assertEquals(VALID_UUID, alreadyValidSession.getAccessTokenId());
        assertEquals(VALID_SHA256, alreadyValidSession.getRefreshTokenHash());
        assertFalse(alreadyValidSession.isRevoked());

        // Assert - Legacy session should be updated and revoked
        assertEquals(legacySession.getAccessTokenId(), UUID.fromString(legacySession.getAccessTokenId()).toString(),
                "Access token ID should be updated to a canonical UUID");
        assertEquals(Sha256.hash("raw-refresh-token"), legacySession.getRefreshTokenHash(), "Refresh token should be SHA-256 hashed");
        assertTrue(legacySession.isRevoked(), "Legacy session should be revoked");

        // Verify repository flush called exactly once
        assertEquals(1, batch.migrated());
        assertFalse(batch.hasNext());
        verify(authenticationSessionRepository, times(1)).flush();
        verify(authenticationSessionRepository).findAll(PageRequest.of(0, 500, Sort.by("id").ascending()));
    }

    @Test
    @DisplayName("Should do nothing and not flush repository when all sessions are already in modern format")
    void run_WhenAllSessionsAlreadyValid_DoesNotFlush() {
        // Arrange
        Authentication validSession1 = createSession(VALID_UUID, VALID_SHA256, false);
        Authentication validSession2 = createSession(UUID.randomUUID().toString(), VALID_SHA256, false);

        when(authenticationSessionRepository.findAll(any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(validSession1, validSession2),
                        PageRequest.of(0, 500, Sort.by("id").ascending()), 2));

        AuthSessionMigrationService.MigrationBatch batch = migrationService.migrateBatch(0);

        assertEquals(0, batch.migrated());
        verify(authenticationSessionRepository, never()).flush();
    }

    @Test
    @DisplayName("Should handle empty repository gracefully without flushing")
    void run_WhenNoSessionsExist_DoesNotFlush() {
        // Arrange
        when(authenticationSessionRepository.findAll(any(Pageable.class))).thenReturn(
                new PageImpl<Authentication>(List.of(), PageRequest.of(0, 500, Sort.by("id").ascending()), 0));

        AuthSessionMigrationService.MigrationBatch batch = migrationService.migrateBatch(0);

        assertEquals(0, batch.migrated());
        assertFalse(batch.hasNext());
        verify(authenticationSessionRepository, never()).flush();
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

    // Helper method to instantiate Authentication for testing
    private Authentication createSession(String accessTokenId, String refreshTokenHash, boolean revoked) {
        Authentication session = new Authentication();
        session.setAccessTokenId(accessTokenId);
        session.setRefreshTokenHash(refreshTokenHash);
        session.setRevoked(revoked);
        return session;
    }
}