package com.hdfclife.capstoneone.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationUnitTest {

    @Test
    @DisplayName("onCreate should initialize createdAt and updatedAt timestamps")
    void onCreateInitializesTimestamps() {
        AuthSession session = new AuthSession();

        assertNull(session.getCreatedAt());
        assertNull(session.getUpdatedAt());

        // Invoke @PrePersist callback directly
        session.onCreate();

        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getUpdatedAt());
        assertEquals(session.getCreatedAt(), session.getUpdatedAt());
    }

    @Test
    @DisplayName("onUpdate should refresh the updatedAt timestamp")
    void onUpdateRefreshesTimestamp() throws InterruptedException {
        AuthSession session = new AuthSession();
        session.onCreate();
        Instant originalUpdatedAt = session.getUpdatedAt();

        // Small delay to ensure timestamp difference
        Thread.sleep(10);

        // Invoke @PreUpdate callback directly
        session.onUpdate();

        assertTrue(session.getUpdatedAt().isAfter(originalUpdatedAt));
        assertEquals(originalUpdatedAt, session.getCreatedAt()); // createdAt remains unchanged
    }
}