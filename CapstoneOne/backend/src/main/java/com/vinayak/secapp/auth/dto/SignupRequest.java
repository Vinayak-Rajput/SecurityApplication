package com.vinayak.secapp.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.vinayak.secapp.common.validation.MaxUtf8Bytes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SignupRequest", description = "Payload used to register a new user"
)
public class SignupRequest {

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "\\S(?:.*\\S)?", message = "Username must not start or end with whitespace")
    @Schema(
            description = "Desired username",
            example = "jdoe",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    @Schema(
            description = "Email address",
            example = "jdoe@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    @MaxUtf8Bytes(max = 72, message = "Password must not exceed 72 UTF-8 bytes")
    @Schema(
            description = "Account password",
            example = "Str0ngPass!",
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.WRITE_ONLY
    )
    private String password;
}
