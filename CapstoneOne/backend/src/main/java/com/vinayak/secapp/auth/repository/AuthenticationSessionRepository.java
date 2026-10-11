package com.vinayak.secapp.auth.repository;

import com.vinayak.secapp.auth.entity.Authentication;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthenticationSessionRepository extends JpaRepository<Authentication, UUID> {

    boolean existsByIdAndRevokedFalseAndAccessTokenExpiresAtAfterAndRefreshSessionExpiresAtAfterAndAccessTokenId(
            UUID id,
            Instant now,
            Instant refreshSessionExpiresAt,
            String accessTokenId
    );

    Slice<Authentication> findAllByIdIsNotNull(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Authentication> findAuthSessionById(UUID id);

    @Modifying
    @Query("update AuthSession session set session.revoked = true where session.username = ?1 and session.revoked = false")
    int revokeAllActiveByUsername(String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from AuthSession session
            where session.revoked = true
               or (session.accessTokenExpiresAt <= :now and session.refreshTokenExpiresAt <= :now)
            """)
    int deleteRevokedOrExpired(@Param("now") Instant now);
}
