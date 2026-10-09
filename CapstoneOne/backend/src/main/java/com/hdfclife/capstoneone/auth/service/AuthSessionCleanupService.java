package com.hdfclife.capstoneone.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthSessionCleanupService {

    private final TokenService tokenService;

    // Retain sessions while either token can still be used; revoked sessions are safe to remove immediately.
    @Scheduled(fixedDelayString = "PT24H", initialDelayString = "PT1H")
    public void removeInactiveSessions() {
        int deleted = tokenService.deleteRevokedOrExpiredSessions(Instant.now());
        if (deleted > 0) {
            log.info("Removed {} revoked or expired authentication sessions", deleted);
        }
    }
}
