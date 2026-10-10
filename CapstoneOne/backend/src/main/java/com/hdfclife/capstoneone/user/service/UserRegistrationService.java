package com.hdfclife.capstoneone.user.service;

import com.hdfclife.capstoneone.auth.dto.SignupResponse;
import com.hdfclife.capstoneone.auth.dto.SignupRequest;
import com.hdfclife.capstoneone.user.entity.User;
import com.hdfclife.capstoneone.user.exception.DuplicateUserException;
import com.hdfclife.capstoneone.common.exception.InvalidRequestException;
import com.hdfclife.capstoneone.logging.ApplicationLogger;
import com.hdfclife.capstoneone.audit.model.AuditEvent;
import com.hdfclife.capstoneone.audit.model.AuditEventType;
import com.hdfclife.capstoneone.user.repository.UserRepository;
import com.hdfclife.capstoneone.user.util.UserIdentityNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationLogger applicationLogger;

    public SignupResponse register(SignupRequest request) {
        User user = createUser(request.getUsername(), request.getEmail(), request.getPassword(), "USER");

        applicationLogger.log(AuditEvent.request(
                AuditEventType.USER_REGISTERED,
                null,
                user.getUsername(),
                "POST",
                "/signup",
                201,
                "Public signup completed"
        ));

        return new SignupResponse("Account created successfully");
    }

    public User createAdminManagedUser(String username, String email, String password, String role) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);

        if (!Set.of("USER", "ADMIN", "AUDITOR").contains(normalizedRole)) {
            throw new InvalidRequestException("Role must be USER, ADMIN, or AUDITOR");
        }

        return createUser(username, email, password, normalizedRole);
    }

    private User createUser(String requestedUsername, String requestedEmail, String password, String role) {
        String username = UserIdentityNormalizer.username(requestedUsername);
        String email = UserIdentityNormalizer.email(requestedEmail);

        List<User> matchingUsers = userRepository.findByUsernameOrEmail(username, email);
        if (matchingUsers.stream().anyMatch(existingUser -> existingUser.getUsername().equals(username))) {
            throw new DuplicateUserException("Username is already registered");
        }

        if (matchingUsers.stream().anyMatch(existingUser -> existingUser.getEmail().equals(email))) {
            throw new DuplicateUserException("Email is already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        user.setLocked(false);

        try {
            return userRepository.saveAndFlush(user);
        }
        catch (DataIntegrityViolationException exception) {
            throw new DuplicateUserException("Username or email is already registered");
        }
    }
}
