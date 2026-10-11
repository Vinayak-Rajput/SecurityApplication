package com.vinayak.secapp.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UserUnitTest {

    @Test
    @DisplayName("Should initialize default field values upon creation")
    void verifyDefaultFieldValues() {
        User user = new User();

        assertEquals("USER", user.getRole());
        assertTrue(user.isEnabled());
        assertFalse(user.isLocked());
        assertNull(user.getId());
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("onCreate should populate createdAt and updatedAt with identical timestamps")
    void onCreatePopulatesTimestamps() {
        User user = new User();

        user.onCreate();

        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertEquals(user.getCreatedAt(), user.getUpdatedAt());
    }

    @Test
    @DisplayName("onUpdate should modify updatedAt without altering createdAt")
    void onUpdateRefreshesTimestamp() throws InterruptedException {
        User user = new User();
        user.onCreate();
        Instant initialCreatedAt = user.getCreatedAt();
        Instant initialUpdatedAt = user.getUpdatedAt();

        Thread.sleep(10); // Ensure measurable delay
        user.onUpdate();

        assertEquals(initialCreatedAt, user.getCreatedAt());
        assertTrue(user.getUpdatedAt().isAfter(initialUpdatedAt));
    }
}