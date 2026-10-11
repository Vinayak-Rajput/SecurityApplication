package com.vinayak.secapp.auth.startup;

import com.vinayak.secapp.auth.entity.Authentication;
import com.vinayak.secapp.auth.repository.AuthenticationSessionRepository;
import com.vinayak.secapp.common.util.Sha256;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthSessionMigrationService {

    private static final int BATCH_SIZE = 500;
    private static final Pattern SHA_256_PATTERN = Pattern.compile("[0-9a-f]{64}");

    private final AuthenticationSessionRepository authenticationSessionRepository;

    @Transactional
    public MigrationBatch migrateBatch(int pageNumber) {
        Slice<Authentication> sessions = authenticationSessionRepository.findAllByIdIsNotNull(
                PageRequest.of(pageNumber, BATCH_SIZE, Sort.by("id").ascending()));
        int migrated = 0;

        for (Authentication session : sessions) {
            if (isTokenId(session.getAccessTokenId())
                    && isSha256(session.getRefreshTokenHash())) {
                continue;
            }

            // Legacy raw credentials cannot be safely retained; replace stored material and force a fresh login.
            session.setAccessTokenId(UUID.randomUUID().toString());
            session.setRefreshTokenHash(Sha256.hash(session.getRefreshTokenHash()));
            session.setRevoked(true);
            migrated++;
        }

        if (migrated > 0) {
            authenticationSessionRepository.flush();
        }
        return new MigrationBatch(migrated, sessions.hasNext());
    }

    private static boolean isTokenId(String value) {
        if (value == null) {
            return false;
        }
        try {
            return UUID.fromString(value).toString().equals(value);
        }
        catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean isSha256(String value) {
        return value != null && SHA_256_PATTERN.matcher(value).matches();
    }

    public record MigrationBatch(int migrated, boolean hasNext) {
    }
}
