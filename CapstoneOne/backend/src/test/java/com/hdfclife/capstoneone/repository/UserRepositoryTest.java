package com.hdfclife.capstoneone.repository;

import com.hdfclife.capstoneone.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setUsername("john_doe");
        user1.setEmail("john@example.com");
        user1.setRole("ADMIN");

        user2 = new User();
        user2.setUsername("jane_smith");
        user2.setEmail("jane@example.com");
        user2.setRole("USER");

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();
    }

    @Nested
    @DisplayName("findByUsername() Tests")
    class FindByUsernameTests {

        @Test
        @DisplayName("Should return User when username exists")
        void findByUsername_ExistingUsername_ReturnsUser() {
            Optional<User> result = userRepository.findByUsername("john_doe");

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Should return empty Optional when username does not exist")
        void findByUsername_NonExistingUsername_ReturnsEmpty() {
            Optional<User> result = userRepository.findByUsername("unknown_user");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEmail() Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Should return User when email exists")
        void findByEmail_ExistingEmail_ReturnsUser() {
            Optional<User> result = userRepository.findByEmail("jane@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("jane_smith");
        }

        @Test
        @DisplayName("Should return empty Optional when email does not exist")
        void findByEmail_NonExistingEmail_ReturnsEmpty() {
            Optional<User> result = userRepository.findByEmail("notfound@example.com");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByUsername() & existsByEmail() Tests")
    class ExistenceCheckTests {

        @Test
        @DisplayName("Should return true when username or email exists, false otherwise")
        void existenceChecks_ValidAndInvalidInputs_ReturnsExpectedBoolean() {
            assertThat(userRepository.existsByUsername("john_doe")).isTrue();
            assertThat(userRepository.existsByUsername("ghost")).isFalse();

            assertThat(userRepository.existsByEmail("john@example.com")).isTrue();
            assertThat(userRepository.existsByEmail("ghost@example.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("findAllByRoleForUpdate() Tests")
    class PessimisticLockQueryTests {

        @Test
        @DisplayName("Should retrieve matching users by role with pessimistic write lock query")
        void findAllByRoleForUpdate_MatchingRole_ReturnsUsers() {
            List<User> admins = userRepository.findAllByRoleForUpdate("ADMIN");

            assertThat(admins).hasSize(1);
            assertThat(admins.get(0).getUsername()).isEqualTo("john_doe");
        }

        @Test
        @DisplayName("Should return empty list when no users have specified role")
        void findAllByRoleForUpdate_NonMatchingRole_ReturnsEmptyList() {
            List<User> guests = userRepository.findAllByRoleForUpdate("GUEST");

            assertThat(guests).isEmpty();
        }
    }
}