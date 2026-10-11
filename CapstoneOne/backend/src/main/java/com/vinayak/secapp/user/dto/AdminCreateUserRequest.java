package com.vinayak.secapp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import com.vinayak.secapp.common.validation.MaxUtf8Bytes;

public record AdminCreateUserRequest(

        @NotBlank(message = "Username must not be blank")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "\\S(?:.*\\S)?", message = "Username must not start or end with whitespace")
        String username,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @MaxUtf8Bytes(max = 72, message = "Password must not exceed 72 UTF-8 bytes")
        @Schema(
                accessMode = Schema.AccessMode.WRITE_ONLY,
                description = "Account password; never returned by the API"
        )
        String password,

        @NotBlank(message = "Role must not be blank")
        @Pattern(regexp = "(?i:USER|ADMIN|AUDITOR)", message = "Role must be USER, ADMIN, or AUDITOR")
        @Schema(
                description = "Role assigned to the new account",
                allowableValues = {"USER", "ADMIN", "AUDITOR"}
        )
        String role
) {
}
