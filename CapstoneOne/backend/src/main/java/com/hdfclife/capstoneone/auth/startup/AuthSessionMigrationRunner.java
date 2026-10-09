package com.hdfclife.capstoneone.auth.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthSessionMigrationRunner implements ApplicationRunner {

    private final AuthSessionMigrationService migrationService;

    @Override
    public void run(ApplicationArguments args) {
        int migrated = 0;
        int pageNumber = 0;
        AuthSessionMigrationService.MigrationBatch batch;

        do {
            batch = migrationService.migrateBatch(pageNumber++);
            migrated += batch.migrated();
        } while (batch.hasNext());

        if (migrated > 0) {
            log.warn("Revoked {} legacy auth sessions containing raw tokens; users must sign in again", migrated);
        }
    }
}
