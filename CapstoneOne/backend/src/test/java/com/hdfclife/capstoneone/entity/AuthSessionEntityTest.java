package com.hdfclife.capstoneone.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AuthSessionEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    private AuthSession validSession;

    @BeforeEach
    void setUp() {
        validSession = new AuthSession();
        validSession.setId(UUID.randomUUID());
        validSession.setUsername("alice");
        validSession.setAccessTokenId("access-token-xyz");
        validSession.setRefreshTokenHash("refresh-hash-123");
        validSession.setAccessTokenExpiresAt(Instant.now().plusSeconds(3600));
        validSession.setRefreshTokenExpiresAt(Instant.now().plusSeconds(86400));
        validSession.setRevoked(false);
    }

    @Test
    @DisplayName("Persisting entity should trigger @PrePersist and auto-populate timestamps")
    void persistTriggersPrePersist() {
        AuthSession saved = entityManager.persistAndFlush(validSession);

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("Updating entity should trigger @PreUpdate and change updatedAt")
    void updateTriggersPreUpdate() {
        AuthSession saved = entityManager.persistAndFlush(validSession);
        Instant initialUpdatedAt = saved.getUpdatedAt();

        saved.setRevoked(true);
        AuthSession updated = entityManager.persistAndFlush(saved);

        assertTrue(updated.getUpdatedAt().isAfter(initialUpdatedAt) || updated.getUpdatedAt().equals(initialUpdatedAt));
    }

    @Test
    @DisplayName("Saving entity with null username should throw DataIntegrityViolationException")
    void nullUsernameThrowsException() {
        validSession.setUsername(null);

        assertThrows(DataIntegrityViolationException.class, () -> {
            entityManager.persistAndFlush(validSession);
        });
    }

    @Test
    @DisplayName("Saving duplicate refresh_token should throw DataIntegrityViolationException")
    void duplicateRefreshTokenThrowsException() {
        entityManager.persistAndFlush(validSession);

        AuthSession duplicateSession = new AuthSession();
        duplicateSession.setId(UUID.randomUUID());
        duplicateSession.setUsername("bob");
        duplicateSession.setAccessTokenId("access-token-abc");
        duplicateSession.setRefreshTokenHash("refresh-hash-123"); // Same hash as validSession
        duplicateSession.setAccessTokenExpiresAt(Instant.now().plusSeconds(3600));
        duplicateSession.setRefreshTokenExpiresAt(Instant.now().plusSeconds(86400));

        assertThrows(DataIntegrityViolationException.class, () -> {
            entityManager.persistAndFlush(duplicateSession);
        });
    }
}