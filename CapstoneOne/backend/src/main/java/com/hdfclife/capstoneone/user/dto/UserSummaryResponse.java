package com.hdfclife.capstoneone.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(
        name = "UserSummaryResponse",
        description = "Account details safe for authorized user and administrator views"
)
public record UserSummaryResponse(

        @Schema(
                description = "Unique account identifier",
                example = "42"
        )
        Long id,

        @Schema(
                description = "Account username",
                example = "jdoe"
        )
        String username,

        @Schema(
                description = "Account email address",
                example = "jdoe@example.com"
        )
        String email,

        @Schema(
                description = "Assigned role",
                example = "USER",
                allowableValues = {"USER", "ADMIN", "AUDITOR"}
        )
        String role,

        @Schema(
                description = "Whether the account is enabled",
                example = "true"
        )
        boolean enabled,

        @Schema(
                description = "Whether the account is locked",
                example = "false"
        )
        boolean locked,

        @Schema(
                description = "Account creation time in UTC",
                example = "2026-09-25T10:15:30Z"
        )
        Instant createdAt,

        @Schema(
                description = "Last account update time in UTC",
                example = "2026-09-25T10:15:30Z"
        )
        Instant updatedAt
) { }
