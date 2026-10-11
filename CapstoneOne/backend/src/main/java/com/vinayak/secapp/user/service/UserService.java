package com.vinayak.secapp.user.service;

import com.vinayak.secapp.user.exception.UserNotFoundException;
import com.vinayak.secapp.user.exception.UserOperationException;
import com.vinayak.secapp.user.entity.User;
import com.vinayak.secapp.user.repository.UserRepository;
import com.vinayak.secapp.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getUser(long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user no longer exists"));
    }

    @Transactional
    public String deleteUser(long id, String requesterUsername) {
        User user = getUser(id);

        if (user.getUsername().equals(requesterUsername)) {
            throw new UserOperationException("Administrators cannot delete their own account");
        }

        if ("ADMIN".equals(user.getRole())) {
            // The row lock and surrounding transaction serialize admin deletions so concurrent requests
            // cannot both observe multiple admins and then delete the final accounts.
            List<User> admins = userRepository.findAllByRole("ADMIN");
            if (admins.size() <= 1) {
                throw new UserOperationException("The last administrator account cannot be deleted");
            }
        }

        tokenService.revokeAllSessions(user.getUsername());
        userRepository.delete(user);
        return user.getUsername();
    }
}
