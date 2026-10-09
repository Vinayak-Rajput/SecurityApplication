package com.hdfclife.capstoneone.auth.controller;

import com.hdfclife.capstoneone.auth.dto.AuthResponse;
import com.hdfclife.capstoneone.common.dto.ErrorResponse;
import com.hdfclife.capstoneone.auth.dto.LoginRequest;
import com.hdfclife.capstoneone.auth.dto.LoginResponse;
import com.hdfclife.capstoneone.common.dto.MessageResponse;
import com.hdfclife.capstoneone.auth.dto.SignupRequest;
import com.hdfclife.capstoneone.auth.security.BearerTokenResolver;
import com.hdfclife.capstoneone.auth.security.RefreshRequestOriginValidator;
import com.hdfclife.capstoneone.auth.security.RefreshTokenCookieFactory;
import com.hdfclife.capstoneone.auth.service.AuthService;
import com.hdfclife.capstoneone.auth.dto.IssuedTokens;
import com.hdfclife.capstoneone.user.service.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "Login, signup, token validation, refresh, and logout APIs"
)
public class AuthController {

    private final AuthService authService;
    private final BearerTokenResolver bearerTokenResolver;
    private final UserRegistrationService userRegistrationService;
    private final RefreshRequestOriginValidator refreshRequestOriginValidator;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates with username and password, returns an access JWT, and sets the refresh JWT as an HttpOnly cookie."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed or bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many login attempts (rate limited)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Authentication database unavailable or circuit breaker open",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected authentication error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        IssuedTokens issuedTokens = authService.login(request, httpRequest.getRemoteAddr());

        return ResponseEntity.ok()
                .header("Set-Cookie", refreshTokenCookieFactory.issue(issuedTokens.refreshToken()).toString())
                .body(new LoginResponse("Login successful", issuedTokens.accessToken(), "Bearer"));
    }

    @PostMapping("/signup")
    @Operation(
            summary = "User signup",
            description = "Registers a new user account with username, email, and password."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Username or email already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Application database unavailable or circuit breaker open",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest request) {
        MessageResponse response = userRegistrationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/auth")
    @Operation(
            summary = "Validate access token",
            description = "Validates the JWT from the Authorization header and returns the authenticated identity."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Token is valid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing, malformed, invalid, expired, or inactive token",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Authenticated request limit exceeded; retry after the Retry-After interval",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Authentication database unavailable or circuit breaker open",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected token validation error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<AuthResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = bearerTokenResolver.resolve(authHeader);
        return ResponseEntity.ok(authService.validateToken(token));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Exchanges the HttpOnly refresh-token cookie for a new access token and rotated refresh-token cookie. Requires an allowed Origin header."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tokens refreshed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Refresh request origin is not allowed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Authenticated request limit exceeded",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Authentication database unavailable or circuit breaker open",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected token refresh error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<LoginResponse> refresh(@CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken, HttpServletRequest request) {
        refreshRequestOriginValidator.requireAllowed(request.getHeader("Origin"));
        IssuedTokens issuedTokens = authService.refresh(refreshToken);

        return ResponseEntity.ok()
                .header("Set-Cookie", refreshTokenCookieFactory.issue(issuedTokens.refreshToken()).toString())
                .body(new LoginResponse("Token refreshed successfully", issuedTokens.accessToken(), "Bearer"));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "User logout",
            description = "Revokes the session using the active access token and clears the refresh-token cookie."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Logout successful",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — missing, malformed, invalid, expired, or inactive token",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many logout attempts; retry after the Retry-After interval",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Authentication database unavailable or circuit breaker open",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected logout error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok()
                .header("Set-Cookie", refreshTokenCookieFactory.clear().toString())
                .body(authService.logout(bearerTokenResolver.resolve(authHeader)));
    }

    @PostMapping("/refresh/logout")
    @Operation(
            summary = "Logout using refresh token",
            description = "Revokes the current session using the HttpOnly refresh-token cookie. Requires an allowed Origin header."
    )
    public ResponseEntity<MessageResponse> logoutWithRefreshToken(
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest request
    ) {
        refreshRequestOriginValidator.requireAllowed(request.getHeader("Origin"));
        MessageResponse response = authService.logoutRefresh(refreshToken);

        return ResponseEntity.ok()
                .header("Set-Cookie", refreshTokenCookieFactory.clear().toString())
                .body(response);
    }
}
