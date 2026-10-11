package com.vinayak.secapp.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AuthenticationResponse", description = "Result of validating a JWT access token")
public class AuthenticationResponse {

    @Schema(description = "Human-readable status message", example = "Token is valid")
    private String message;

    @Schema(description = "Authenticated username extracted from the token", example = "admin")
    private String username;

    @Schema(description = "Whether the caller is authenticated", example = "true")
    private boolean authenticated;
}
