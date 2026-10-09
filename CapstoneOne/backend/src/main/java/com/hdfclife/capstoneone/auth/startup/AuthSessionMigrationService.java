package com.hdfclife.capstoneone.auth.startup;

import com.hdfclife.capstoneone.auth.entity.AuthSession;
import com.hdfclife.capstoneone.auth.repository.AuthSessionRepository;
import com.hdfclife.capstoneone.common.util.Sha256;
import com.hdfclife.capstoneone.auth.util.TokenFormats;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthSessionMigrationService {

    private static final int BATCH_SIZE = 500;

    private final AuthSessionRepository authSessionRepository;

    @Transactional
    public MigrationBatch migrateBatch(int pageNumber) {
        Slice<AuthSession> sessions = authSessionRepository.findAllByIdIsNotNull(
                PageRequest.of(pageNumber, BATCH_SIZE, Sort.by("id").ascending()));
        int migrated = 0;

        for (AuthSession session : sessions) {
            if (TokenFormats.isTokenId(session.getAccessTokenId())
                    && TokenFormats.isSha256(session.getRefreshTokenHash())) {
                continue;
            }

            // Legacy raw credentials cannot be safely retained; replace stored material and force a fresh login.
            session.setAccessTokenId(UUID.randomUUID().toString());
            session.setRefreshTokenHash(Sha256.hash(session.getRefreshTokenHash()));
            session.setRevoked(true);
            migrated++;
        }

        if (migrated > 0) {
            authSessionRepository.flush();
        }
        return new MigrationBatch(migrated, sessions.hasNext());
    }

    public record MigrationBatch(int migrated, boolean hasNext) {
    }
}
