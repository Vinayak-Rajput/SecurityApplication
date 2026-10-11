package com.vinayak.secapp.user.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    private User validUser;

    @BeforeEach
    void setUp() {
        validUser = new User();
        validUser.setUsername("vinayak");
        validUser.setEmail("vinayak@example.com");
        validUser.setPassword("$2a$10$e8A2f...hashedPassword");
    }

    @Test
    @DisplayName("Persisting valid user should auto-generate ID and execute @PrePersist")
    void persistValidUserSuccess() {
        User savedUser = entityManager.persistAndFlush(validUser);

        assertNotNull(savedUser.getId());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
        assertEquals("USER", savedUser.getRole());
        assertTrue(savedUser.isEnabled());
        assertFalse(savedUser.isLocked());
    }

    @Test
    @DisplayName("Persisting user with duplicate username should throw DataIntegrityViolationException")
    void duplicateUsernameThrowsException() {
        entityManager.persistAndFlush(validUser);

        User duplicateUsernameUser = new User();
        duplicateUsernameUser.setUsername("vinayak"); // Duplicate
        duplicateUsernameUser.setEmail("other@example.com");
        duplicateUsernameUser.setPassword("password123");

        assertThrows(DataIntegrityViolationException.class, () -> {
            entityManager.persistAndFlush(duplicateUsernameUser);
        });
    }

    @Test
    @DisplayName("Persisting user with duplicate email should throw DataIntegrityViolationException")
    void duplicateEmailThrowsException() {
        entityManager.persistAndFlush(validUser);

        User duplicateEmailUser = new User();
        duplicateEmailUser.setUsername("otheruser");
        duplicateEmailUser.setEmail("vinayak@example.com"); // Duplicate
        duplicateEmailUser.setPassword("password123");

        assertThrows(DataIntegrityViolationException.class, () -> {
            entityManager.persistAndFlush(duplicateEmailUser);
        });
    }

    @Test
    @DisplayName("Persisting user with null password should throw DataIntegrityViolationException")
    void nullPasswordThrowsException() {
        validUser.setPassword(null);

        assertThrows(DataIntegrityViolationException.class, () -> {
            entityManager.persistAndFlush(validUser);
        });
    }

    @Test
    @DisplayName("Updating user attributes should trigger @PreUpdate")
    void updatingUserTriggersPreUpdate() {
        User savedUser = entityManager.persistAndFlush(validUser);
        var initialUpdatedAt = savedUser.getUpdatedAt();

        savedUser.setLocked(true);
        User updatedUser = entityManager.persistAndFlush(savedUser);

        assertTrue(updatedUser.isLocked());
        assertTrue(updatedUser.getUpdatedAt().isAfter(initialUpdatedAt) || 
                   updatedUser.getUpdatedAt().equals(initialUpdatedAt));
    }
}