package com.hdfclife.capstoneone.auth.service;

import com.hdfclife.capstoneone.auth.dto.AuthenticationResponse;
import com.hdfclife.capstoneone.auth.dto.LoginRequest;
import com.hdfclife.capstoneone.auth.dto.LogoutResponse;
import com.hdfclife.capstoneone.auth.dto.SignupResponse;
import com.hdfclife.capstoneone.auth.exception.InactiveTokenException;
import com.hdfclife.capstoneone.auth.exception.InvalidCredentialsException;
import com.hdfclife.capstoneone.auth.exception.InvalidTokenException;
import com.hdfclife.capstoneone.logging.ApplicationLogger;
import com.hdfclife.capstoneone.audit.model.AuditEvent;
import com.hdfclife.capstoneone.audit.model.AuditEventType;
import com.hdfclife.capstoneone.user.repository.UserRepository;
import com.hdfclife.capstoneone.common.util.Sha256;
import com.hdfclife.capstoneone.user.util.UserIdentityNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    public record IssuedTokens(String accessToken, String refreshToken) {
    }

    private final JwtService jwtService;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationLogger applicationLogger;

    public IssuedTokens login(LoginRequest request, String clientIp) {

        return authenticate(request);
    }

    private IssuedTokens authenticate(LoginRequest request) {

        String username = UserIdentityNormalizer.username(request.getUsername());
        String password = request.getPassword();

        var user = userRepository.findByUsernameAndEnabledTrueAndLockedFalse(username)
                .orElseThrow(() -> new InvalidCredentialsException("Incorrect username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Incorrect username or password");
        }

        UUID sessionId = UUID.randomUUID();
        Instant sessionExpiresAt = jwtService.newSessionExpiration();
        String token = jwtService.generateToken(user.getUsername(), sessionId, sessionExpiresAt);
        String refreshToken = jwtService.generateRefreshToken(user.getUsername(), sessionId, sessionExpiresAt);

        JwtService.TokenDetails accessDetails = jwtService.parseToken(token);
        JwtService.TokenDetails refreshDetails = jwtService.parseToken(refreshToken);

        tokenService.saveSession(
            sessionId,
            user.getUsername(),
            accessDetails.tokenId(),
            Sha256.hash(refreshToken),
            accessDetails.expiresAt(),
            refreshDetails.expiresAt(),
            sessionExpiresAt
        );

        applicationLogger.log(
            AuditEvent.authentication(
                AuditEventType.LOGIN_SUCCESS,
                user.getUsername(),
                "User login successful"
            )
        );

        return new IssuedTokens(token, refreshToken);
    }

    public AuthenticationResponse validateToken(String token) {

        JwtService.TokenDetails tokenDetails = jwtService.parseToken(token);
        String username = tokenDetails.username();

        applicationLogger.log(
            AuditEvent.authentication(
                AuditEventType.TOKEN_VALIDATION_SUCCESS,
                username,
                "Token validation successful"
            )
        );

        return new AuthenticationResponse("Authentication successful", username, true);
    }

    public IssuedTokens refresh(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token must not be empty");
        }

        JwtService.TokenDetails presentedToken = jwtService.parseToken(refreshToken);

        if (!presentedToken.refreshToken()) {
            throw new InvalidTokenException("Token is not a refresh token");
        }

        UUID sessionId = presentedToken.sessionId();
        String username = presentedToken.username();
        Instant sessionExpiresAt = presentedToken.sessionExpiresAt();

        if (userRepository.findByUsernameAndEnabledTrueAndLockedFalse(username).isEmpty()) {
            throw new InactiveTokenException("Refresh token is inactive. Please login again.");
        }

        String newAccessToken = jwtService.generateToken(username, sessionId, sessionExpiresAt);
        String newRefreshToken = jwtService.generateRefreshToken(username, sessionId, sessionExpiresAt);

        JwtService.TokenDetails accessDetails = jwtService.parseToken(newAccessToken);
        JwtService.TokenDetails refreshDetails = jwtService.parseToken(newRefreshToken);

        boolean rotated = tokenService.rotateTokens(
            sessionId,
            Sha256.hash(refreshToken),
            accessDetails.tokenId(),
            accessDetails.expiresAt(),
            Sha256.hash(newRefreshToken),
            refreshDetails.expiresAt(),
            sessionExpiresAt
        );

        if (!rotated) {
            throw new InactiveTokenException("Refresh token is inactive. Please login again.");
        }

        applicationLogger.log(
            AuditEvent.authentication(
                AuditEventType.TOKEN_REFRESH_SUCCESS,
                username,
                "Access token refreshed successfully"
            )
        );

        return new IssuedTokens(newAccessToken, newRefreshToken);
    }

    public LogoutResponse logout(String token) {

        JwtService.TokenDetails tokenDetails = jwtService.parseToken(token);

        if (tokenDetails.refreshToken()) {
            throw new InvalidTokenException("Token is not an access token");
        }

        if (!tokenService.revokeSession(tokenDetails.sessionId(), tokenDetails.tokenId())) {
            throw new InactiveTokenException("Token is already inactive");
        }

        applicationLogger.log(
            AuditEvent.authentication(
                AuditEventType.LOGOUT_SUCCESS,
                tokenDetails.username(),
                "User logout successful"
            )
        );

        return new LogoutResponse("Logged out successfully");
    }

    public LogoutResponse logoutRefresh(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token must not be empty");
        }

        JwtService.TokenDetails tokenDetails = jwtService.parseToken(refreshToken);

        if (!tokenDetails.refreshToken()) {
            throw new InvalidTokenException("Token is not a refresh token");
        }

        if (!tokenService.revokeRefreshSession(tokenDetails.sessionId(), Sha256.hash(refreshToken))) {
            throw new InactiveTokenException("Refresh token is already inactive");
        }

        applicationLogger.log(
            AuditEvent.authentication(
                AuditEventType.LOGOUT_SUCCESS,
                tokenDetails.username(),
                "User logout successful"
            )
        );

        return new LogoutResponse("Logged out successfully");
    }
}
