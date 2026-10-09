package com.hdfclife.capstoneone.service;

import com.hdfclife.capstoneone.user.exception.UserOperationException;
import com.hdfclife.capstoneone.user.entity.User;
import com.hdfclife.capstoneone.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TokenService tokenService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, tokenService);
    }

    @Test
    void getUserReturnsExistingAccountAndFailsForMissingId() {
        User user = user("alice", "USER");
        when(userRepository.findById(12L)).thenReturn(Optional.of(user));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertEquals("alice", userService.getUser(12L).getUsername());
        assertThrows(com.hdfclife.capstoneone.user.exception.UserNotFoundException.class,
                () -> userService.getUser(99L));
    }

    @Test
    void getAllUsersReturnsRequestedPage() {
        Pageable pageable = PageRequest.of(1, 20);
        Page<User> expectedPage = new PageImpl<>(List.of(user("alice", "USER")), pageable, 21);
        when(userRepository.findAll(pageable)).thenReturn(expectedPage);

        assertEquals(expectedPage, userService.getAllUsers(pageable));
        verify(userRepository).findAll(pageable);
    }

    @Test
    void administratorCannotDeleteTheirOwnAccount() {
        User admin = user("admin", "ADMIN");
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));

        assertThrows(UserOperationException.class, () -> userService.deleteUser(7L, "admin"));

        verify(tokenService, never()).revokeAllSessions("admin");
        verify(userRepository, never()).delete(admin);
    }

    @Test
    void lastAdministratorCannotBeDeleted() {
        User admin = user("admin", "ADMIN");
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));
        when(userRepository.findAllByRoleForUpdate("ADMIN")).thenReturn(List.of(admin));

        assertThrows(UserOperationException.class, () -> userService.deleteUser(7L, "another-admin"));

        verify(tokenService, never()).revokeAllSessions("admin");
        verify(userRepository, never()).delete(admin);
    }

    @Test
    void deletingAnotherUserRevokesSessionsBeforeRemovingAccount() {
        User user = user("alice", "USER");
        when(userRepository.findById(12L)).thenReturn(Optional.of(user));

        assertEquals("alice", userService.deleteUser(12L, "admin"));

        InOrder order = inOrder(tokenService, userRepository);
        order.verify(tokenService).revokeAllSessions("alice");
        order.verify(userRepository).delete(user);
    }

    @Test
    void administratorCanBeDeletedWhenAnotherAdministratorExists() {
        User target = user("admin-two", "ADMIN");
        User otherAdmin = user("admin-one", "ADMIN");
        when(userRepository.findById(12L)).thenReturn(Optional.of(target));
        when(userRepository.findAllByRoleForUpdate("ADMIN")).thenReturn(List.of(target, otherAdmin));

        assertEquals("admin-two", userService.deleteUser(12L, "admin-one"));
        verify(tokenService).revokeAllSessions("admin-two");
        verify(userRepository).delete(target);
    }

    private static User user(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        return user;
    }
}
