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
@Schema(name = "LoginResponse", description = "Successful auth payload containing the access JWT")
public class LoginResponse {

    @Schema(description = "Human-readable status message", example = "Login successful")
    private String message;

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.signature")
    private String token;

    @Schema(description = "Token type for the Authorization header", example = "Bearer")
    private String tokenType;

}
