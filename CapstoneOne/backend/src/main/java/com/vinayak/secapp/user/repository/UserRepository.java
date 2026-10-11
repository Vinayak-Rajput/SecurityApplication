package com.vinayak.secapp.user.repository;

import com.vinayak.secapp.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndEnabledTrueAndLockedFalse(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByUsernameOrEmail(String username, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<User> findAllByRole(String role);
}
