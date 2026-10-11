package com.vinayak.secapp.user.service;

import com.vinayak.secapp.audit.model.AuditEvent;
import com.vinayak.secapp.audit.model.AuditEventType;
import com.vinayak.secapp.auth.dto.SignupRequest;
import com.vinayak.secapp.auth.dto.SignupResponse;
import com.vinayak.secapp.common.exception.InvalidRequestException;
import com.vinayak.secapp.logging.ApplicationLogger;
import com.vinayak.secapp.user.entity.User;
import com.vinayak.secapp.user.exception.DuplicateUserException;
import com.vinayak.secapp.user.repository.UserRepository;
import com.vinayak.secapp.user.service.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

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
    @Mock private ApplicationLogger applicationLogger;

    private UserRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new UserRegistrationService(userRepository, passwordEncoder, applicationLogger);
    }

    @Test
    void publicSignupNormalizesIdentityHashesPasswordAndLogsRegistration() {
        when(userRepository.findByUsernameOrEmail("alice", "alice@example.com")).thenReturn(List.of());
        when(passwordEncoder.encode("long-secret")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SignupResponse response = registrationService.register(
                new SignupRequest(" alice ", " ALICE@EXAMPLE.COM ", "long-secret"));

        assertEquals("Account created successfully", response.message());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("alice", savedUser.getUsername());
        assertEquals("alice@example.com", savedUser.getEmail());
        assertEquals("bcrypt-hash", savedUser.getPassword());
        assertEquals("USER", savedUser.getRole());
        ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(applicationLogger).log(eventCaptor.capture());
        assertEquals(AuditEventType.USER_REGISTERED, eventCaptor.getValue().type());
        assertEquals("alice", eventCaptor.getValue().subjectUsername());
    }

    @Test
    void duplicateUsernameIsRejectedBeforePasswordHashingOrSaving() {
        when(userRepository.findByUsernameOrEmail("alice", "alice@example.com"))
                .thenReturn(List.of(user("alice", "other@example.com")));

        assertThrows(DuplicateUserException.class, () -> registrationService.register(
                new SignupRequest("alice", "alice@example.com", "long-secret")));

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateEmailIsRejected() {
        when(userRepository.findByUsernameOrEmail("alice", "alice@example.com"))
                .thenReturn(List.of(user("someone", "alice@example.com")));

        assertThrows(DuplicateUserException.class, () -> registrationService.register(
                new SignupRequest("alice", "alice@example.com", "long-secret")));

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void databaseUniquenessRaceIsTranslatedToDuplicateUser() {
        when(userRepository.findByUsernameOrEmail("alice", "alice@example.com")).thenReturn(List.of());
        when(passwordEncoder.encode("long-secret")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("unique"));

        assertThrows(DuplicateUserException.class, () -> registrationService.register(
                new SignupRequest("alice", "alice@example.com", "long-secret")));
    }

    @Test
    void adminManagedAccountAcceptsAndNormalizesKnownRoles() {
        when(userRepository.findByUsernameOrEmail("auditor", "auditor@example.com")).thenReturn(List.of());
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

        verify(userRepository, never()).findByUsernameOrEmail(any(), any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    private static User user(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }
}
