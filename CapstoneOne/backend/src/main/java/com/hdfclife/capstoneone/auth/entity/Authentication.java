package com.hdfclife.capstoneone.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "auth_sessions")
public class Authentication {

    @Setter
    @Id
    private UUID id;

    @Setter
    @Column(nullable = false, length = 50)
    private String username;

    @Setter
    @Column(name = "access_token", nullable = false, length = 2048)
    private String accessTokenId;

    @Setter
    @Column(name = "refresh_token", nullable = false, unique = true, length = 2048)
    private String refreshTokenHash;

    @Setter
    @Column(nullable = false)
    private Instant accessTokenExpiresAt;

    @Setter
    @Column(nullable = false)
    private Instant refreshTokenExpiresAt;

    @Setter
    @Column(nullable = false, updatable = false)
    private Instant refreshSessionExpiresAt;

    @Setter
    @Column(nullable = false)
    private boolean revoked;

    @Setter
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

}
