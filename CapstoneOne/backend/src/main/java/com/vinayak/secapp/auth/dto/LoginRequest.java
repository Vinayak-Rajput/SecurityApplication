package com.vinayak.secapp.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.vinayak.secapp.common.validation.MaxUtf8Bytes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LoginRequest", description = "Credentials used to authenticate a user")
public class LoginRequest {

    @NotBlank(message = "Username must not be blank")
    @Size(max = 50, message = "Username must not exceed 50 characters")
    @Pattern(regexp = "\\S(?:.*\\S)?", message = "Username must not start or end with whitespace")
    @Schema(
        description = "Account username",
        example = "admin",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @NotBlank(message = "Password must not be blank")
    @Size(max = 72, message = "Password must not exceed 72 characters")
    @MaxUtf8Bytes(max = 72, message = "Password must not exceed 72 UTF-8 bytes")
    @Schema(
        description = "Account password",
        example = "Str0ngPass!",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.WRITE_ONLY
    )
    private String password;
}
