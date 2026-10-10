package com.hdfclife.capstoneone.auth.service;

import com.hdfclife.capstoneone.auth.entity.Authentication;
import com.hdfclife.capstoneone.auth.repository.AuthenticationSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final AuthenticationSessionRepository authenticationSessionRepository;

    @Transactional
    public void saveSession(UUID sessionId, String username, String accessTokenId, String refreshTokenHash,
                            Instant accessTokenExpiresAt, Instant refreshTokenExpiresAt,
                            Instant refreshSessionExpiresAt) {
        Authentication session = new Authentication();

        session.setId(sessionId);
        session.setUsername(username);
        session.setAccessTokenId(accessTokenId);
        session.setRefreshTokenHash(refreshTokenHash);
        session.setAccessTokenExpiresAt(accessTokenExpiresAt);
        session.setRefreshTokenExpiresAt(refreshTokenExpiresAt);
        session.setRefreshSessionExpiresAt(refreshSessionExpiresAt);
        session.setRevoked(false);

        authenticationSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public boolean isAccessTokenActive(UUID sessionId, String accessTokenId) {
        Instant now = Instant.now();
        return authenticationSessionRepository.existsByIdAndRevokedFalseAndAccessTokenExpiresAtAfterAndRefreshSessionExpiresAtAfterAndAccessTokenId(
                sessionId,
                now,
                now,
                accessTokenId
        );
    }

    @Transactional
    public boolean rotateTokens(UUID sessionId, String presentedRefreshTokenHash, String newAccessTokenId,
                                Instant newAccessTokenExpiresAt, String newRefreshTokenHash,
                                Instant newRefreshTokenExpiresAt, Instant refreshSessionExpiresAt) {
        // Hold a write lock across verification and replacement so only one concurrent refresh can rotate a session.
        Authentication session = authenticationSessionRepository.findAuthSessionById(sessionId).orElse(null);

        Instant now = Instant.now();
        if (session == null || session.isRevoked()
                || !session.getRefreshTokenExpiresAt().isAfter(now)
                || !session.getRefreshSessionExpiresAt().equals(refreshSessionExpiresAt)
                || !session.getRefreshSessionExpiresAt().isAfter(now)) {
            return false;
        }

        // Compare fixed-length stored hashes in constant time; stale parallel requests fail without revoking the winner.
        if (!MessageDigest.isEqual(session.getRefreshTokenHash().getBytes(StandardCharsets.UTF_8), presentedRefreshTokenHash.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }

        session.setAccessTokenId(newAccessTokenId);
        session.setAccessTokenExpiresAt(newAccessTokenExpiresAt);
        session.setRefreshTokenHash(newRefreshTokenHash);
        // Rotation may extend the idle expiry, but never beyond the fixed login-session deadline.
        session.setRefreshTokenExpiresAt(
                newRefreshTokenExpiresAt.isBefore(refreshSessionExpiresAt)
                        ? newRefreshTokenExpiresAt
                        : refreshSessionExpiresAt
        );

        return true;
    }

    @Transactional
    public boolean revokeSession(UUID sessionId, String accessTokenId) {
        Authentication session = authenticationSessionRepository.findAuthSessionById(sessionId).orElse(null);

        if (session == null || session.isRevoked() || !session.getAccessTokenId().equals(accessTokenId) || !session.getAccessTokenExpiresAt().isAfter(Instant.now())) {
            return false;
        }
        session.setRevoked(true);

        return true;
    }

    @Transactional
    public boolean revokeRefreshSession(UUID sessionId, String presentedRefreshTokenHash) {
        // Lock before checking the hash so logout cannot race a refresh rotation and revoke a different token state.
        Authentication session = authenticationSessionRepository.findAuthSessionById(sessionId).orElse(null);

        Instant now = Instant.now();
        if (session == null || session.isRevoked()
                || !session.getRefreshTokenExpiresAt().isAfter(now)
                || !session.getRefreshSessionExpiresAt().isAfter(now)) {
            return false;
        }

        boolean matches = MessageDigest.isEqual(
                session.getRefreshTokenHash().getBytes(StandardCharsets.UTF_8),
                presentedRefreshTokenHash.getBytes(StandardCharsets.UTF_8)
        );
        if (!matches) {
            return false;
        }

        session.setRevoked(true);
        return true;
    }

    @Transactional
    public int revokeAllSessions(String username) {
        return authenticationSessionRepository.revokeAllActiveByUsername(username);
    }

    @Transactional
    public int deleteRevokedOrExpiredSessions(Instant now) {
        return authenticationSessionRepository.deleteRevokedOrExpired(now);
    }

}
