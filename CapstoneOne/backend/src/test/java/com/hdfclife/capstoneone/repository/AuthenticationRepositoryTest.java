package com.hdfclife.capstoneone.repository;

import com.hdfclife.capstoneone.auth.entity.Authentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AuthenticationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    private Authentication activeSession;
    private Authentication revokedSession;

    @BeforeEach
    void setUp() {
        activeSession = new Authentication();
        activeSession.setUsername("alice");
        activeSession.setRevoked(false);
        activeSession.setCreatedAt(OffsetDateTime.now().toInstant());

        revokedSession = new Authentication();
        revokedSession.setUsername("alice");
        revokedSession.setRevoked(true);
        revokedSession.setCreatedAt(OffsetDateTime.now().toInstant());

        entityManager.persist(activeSession);
        entityManager.persist(revokedSession);
        entityManager.flush();
    }

    @Test
    @DisplayName("findByIdAndRevokedFalse should return session when present and active")
    void findByIdAndRevokedFalseReturnsActiveSession() {
        Optional<Authentication> found = authSessionRepository.findByIdAndRevokedFalse(activeSession.getId());

        assertTrue(found.isPresent());
        assertEquals(activeSession.getId(), found.get().getId());
        assertFalse(found.get().isRevoked());
    }

    @Test
    @DisplayName("findByIdAndRevokedFalse should return empty when session is revoked")
    void findByIdAndRevokedFalseReturnsEmptyWhenRevoked() {
        Optional<Authentication> found = authSessionRepository.findByIdAndRevokedFalse(revokedSession.getId());

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("findByIdForUpdate should retrieve session with pessimistic lock query")
    void findByIdForUpdateReturnsSession() {
        Optional<Authentication> found = authSessionRepository.findByIdForUpdate(activeSession.getId());

        assertTrue(found.isPresent());
        assertEquals("alice", found.get().getUsername());
    }

    @Test
    @DisplayName("revokeAllActiveByUsername should update only active sessions for the user")
    void revokeAllActiveByUsernameRevokesOnlyActive() {
        // Create another active session for a different user
        Authentication bobSession = new Authentication();
        bobSession.setUsername("bob");
        bobSession.setRevoked(false);
        bobSession.setCreatedAt(OffsetDateTime.now().toInstant());
        entityManager.persist(bobSession);
        entityManager.flush();

        int updatedCount = authSessionRepository.revokeAllActiveByUsername("alice");

        // Should update exactly 1 session (alice's active session)
        assertEquals(1, updatedCount);

        entityManager.clear(); // Clear persistence context to fetch updated state from DB

        Authentication reloadedAliceActive = entityManager.find(Authentication.class, activeSession.getId());
        Authentication reloadedBobSession = entityManager.find(Authentication.class, bobSession.getId());

        assertTrue(reloadedAliceActive.isRevoked());
        assertFalse(reloadedBobSession.isRevoked());
    }
}