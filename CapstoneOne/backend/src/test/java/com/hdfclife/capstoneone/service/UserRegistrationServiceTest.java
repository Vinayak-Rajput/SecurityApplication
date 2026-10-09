package com.hdfclife.capstoneone.service;

import com.hdfclife.capstoneone.auth.dto.SignupRequest;
import com.hdfclife.capstoneone.user.exception.DuplicateUserException;
import com.hdfclife.capstoneone.common.exception.InvalidRequestException;
import com.hdfclife.capstoneone.logging.AuditEventLogger;
import com.hdfclife.capstoneone.user.entity.User;
import com.hdfclife.capstoneone.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditEventLogger auditEventLogger;

    private UserRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new UserRegistrationService(userRepository, passwordEncoder, auditEventLogger);
    }

    @Test
    void publicSignupNormalizesIdentityHashesPasswordAndAlwaysCreatesUserRole() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("long-secret")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registrationService.register(new SignupRequest(" alice ", " ALICE@EXAMPLE.COM ", "long-secret"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertEquals("alice", saved.getValue().getUsername());
        assertEquals("alice@example.com", saved.getValue().getEmail());
        assertEquals("bcrypt-hash", saved.getValue().getPassword());
        assertEquals("USER", saved.getValue().getRole());
        verify(passwordEncoder).encode("long-secret");
    }

    @Test
    void duplicateUsernameStopsBeforePasswordHashingOrSaving() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> registrationService.register(
                new SignupRequest("alice", "alice@example.com", "long-secret")));

        verify(userRepository, never()).existsByEmail(any());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateEmailIsRejected() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> registrationService.register(
                new SignupRequest("alice", "alice@example.com", "long-secret")));

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void databaseUniquenessRaceIsTranslatedToDuplicateUser() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("long-secret")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("unique"));

        assertThrows(DuplicateUserException.class, () -> registrationService.register(
                new SignupRequest("alice", "alice@example.com", "long-secret")));
    }

    @Test
    void adminManagedAccountAcceptsOnlyKnownRolesAndNormalizesRole() {
        when(userRepository.existsByUsername("auditor")).thenReturn(false);
        when(userRepository.existsByEmail("auditor@example.com")).thenReturn(false);
        when(passwordEncoder.encode("long-secret")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = registrationService.createAdminManagedUser(
                "auditor", "AUDITOR@example.com", "long-secret", " auditor ");

        assertEquals("AUDITOR", created.getRole());
        assertEquals("auditor@example.com", created.getEmail());
    }

    @Test
    void adminManagedAccountRejectsUnknownRoleBeforeDatabaseAccess() {
        assertThrows(InvalidRequestException.class, () -> registrationService.createAdminManagedUser(
                "alice", "alice@example.com", "long-secret", "SUPERADMIN"));

        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).saveAndFlush(any());
    }
}
