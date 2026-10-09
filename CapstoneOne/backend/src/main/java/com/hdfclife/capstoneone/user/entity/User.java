package com.hdfclife.capstoneone.user.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "users")
@Schema(
        name = "User",
        description = "Registered application user"
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(
            description = "User id",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @Setter
    @Column(nullable = false, unique = true, length = 50)
    @Schema(
            description = "Username",
            example = "vinayak"
    )
    private String username;

    @Setter
    @Column(nullable = false, unique = true, length = 254)
    @Schema(
            description = "Email address",
            example = "vinayak@example.com"
    )
    private String email;

    @Setter
    @Column(nullable = false, length = 100)
    @Schema(
            description = "Hashed password",
            accessMode = Schema.AccessMode.WRITE_ONLY
    )
    private String password;

    @Setter
    @Column(nullable = false, length = 20)
    @Schema(
            description = "Assigned role",
            example = "USER"
    )
    private String role = "USER";

    @Setter
    @Column(nullable = false)
    @Schema(
            description = "Whether the account is enabled",
            example = "true"
    )
    private boolean enabled = true;

    @Setter
    @Column(nullable = false)
    @Schema(
            description = "Whether the account is locked",
            example = "false"
    )
    private boolean locked = false;

    @Column(nullable = false, updatable = false)
    @Schema(
            description = "Creation timestamp",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Instant createdAt;

    @Column(nullable = false)
    @Schema(
            description = "Last update timestamp",
            accessMode = Schema.AccessMode.READ_ONLY
    )
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

    public User() {
    }

}
