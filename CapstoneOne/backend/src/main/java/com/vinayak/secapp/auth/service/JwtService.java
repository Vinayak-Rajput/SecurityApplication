package com.vinayak.secapp.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinayak.secapp.auth.config.properties.JwtProperties;
import com.vinayak.secapp.auth.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String CLAIM_SESSION_ID = "sid";
    private static final String CLAIM_SESSION_EXPIRATION = "session_exp";

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtService(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public TokenDetails parseToken(String token) {
        Claims claims = parseClaims(token);
        String sessionValue = claims.get(CLAIM_SESSION_ID, String.class);
        UUID sessionId;
        try {
            sessionId = UUID.fromString(sessionValue);
        }
        catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidTokenException("Token has no valid session identifier");
        }

        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw new InvalidTokenException("Token has no identifier");
        }

        Date expirationDate = claims.getExpiration();
        if (expirationDate == null) {
            throw new InvalidTokenException("Token has no expiration");
        }

        Date sessionExpirationDate = claims.get(CLAIM_SESSION_EXPIRATION, Date.class);
        if (sessionExpirationDate == null || expirationDate.after(sessionExpirationDate)) {
            throw new InvalidTokenException("Token has no valid session expiration");
        }

        String username = claims.getSubject();
        if (username == null || username.isBlank()) {
            throw new InvalidTokenException("Token has no subject");
        }

        String tokenType = claims.get(CLAIM_TYPE, String.class);
        if (!TOKEN_TYPE_ACCESS.equals(tokenType) && !TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new InvalidTokenException("Token has no valid type");
        }
        String expectedAudience = TOKEN_TYPE_REFRESH.equals(tokenType)
                ? properties.refreshAudience()
                : properties.accessAudience();
        if (!properties.issuer().equals(claims.getIssuer())
                || claims.getAudience() == null
                || !claims.getAudience().contains(expectedAudience)) {
            throw new InvalidTokenException("Token issuer or audience is invalid");
        }

        return new TokenDetails(
                username,
                sessionId,
                tokenId,
                expirationDate.toInstant(),
                sessionExpirationDate.toInstant(),
                TOKEN_TYPE_REFRESH.equals(tokenType));
    }

    public String extractUsername(String token) {
        return parseToken(token).username();
    }

    public boolean isTokenValid(String token) {
        parseToken(token);
        return true;
    }

    public boolean isTokenValid(String token, String username) {
        return parseToken(token).username().equals(username);
    }

    public Date extractExpiration(String token) {
        return Date.from(parseToken(token).expiresAt());
    }

    public UUID extractSessionId(String token) {
        return parseToken(token).sessionId();
    }

    public String extractTokenId(String token) {
        return parseToken(token).tokenId();
    }

    public boolean isRefreshToken(String token) {
        return parseToken(token).refreshToken();
    }

    private SecretKey getSigningKey(String keyId) {
        try {
            return Keys.hmacShaKeyFor(properties.signingKey(keyId));
        }
        catch (IllegalArgumentException exception) {
            throw new InvalidTokenException("Token uses an unknown signing key");
        }
    }

    private Claims parseClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token must not be empty");
        }

        // The unverified key ID selects only from configured keys; JJWT still verifies the signature before returning claims.
        String keyId = readKeyId(token);
        return Jwts.parser()
                .verifyWith(getSigningKey(keyId))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String readKeyId(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) {
                throw new InvalidTokenException("Token is malformed");
            }
            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode keyId = header == null ? null : header.get("kid");
            if (keyId == null || !keyId.isTextual() || keyId.asText().isBlank()) {
                throw new InvalidTokenException("Token has no signing key identifier");
            }
            return keyId.asText();
        }
        catch (IOException | IllegalArgumentException exception) {
            throw new InvalidTokenException("Token has an invalid signing key identifier");
        }
    }

    public String generateToken(String username, UUID sessionId, Instant sessionExpiresAt) {
        Date now = new Date();
        Date expiryDate = Date.from(min(now.toInstant().plus(properties.expiration()), sessionExpiresAt));

        return Jwts.builder()
                .header().keyId(properties.activeKeyId()).and()
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .claim(CLAIM_SESSION_ID, sessionId.toString())
                .claim(CLAIM_SESSION_EXPIRATION, Date.from(sessionExpiresAt))
                .issuer(properties.issuer())
                .audience().add(properties.accessAudience()).and()
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(properties.activeKeyId()))
                .compact();
    }

    public String generateRefreshToken(String username, UUID sessionId, Instant sessionExpiresAt) {
        Date now = new Date();
        Date expiryDate = Date.from(min(now.toInstant().plus(properties.refreshExpiration()), sessionExpiresAt));

        return Jwts.builder()
                .header().keyId(properties.activeKeyId()).and()
                .subject(username)
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .claim(CLAIM_SESSION_ID, sessionId.toString())
                .claim(CLAIM_SESSION_EXPIRATION, Date.from(sessionExpiresAt))
                .issuer(properties.issuer())
                .audience().add(properties.refreshAudience()).and()
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString())
                .signWith(getSigningKey(properties.activeKeyId()))
                .compact();
    }

    public Instant newSessionExpiration() {
        // Keep this deadline millisecond-aligned because JWT NumericDate values have second/millisecond precision.
        return Instant.now().plus(properties.refreshMaxLifetime())
                .truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
    }

    private static Instant min(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    public record TokenDetails(
            String username,
            UUID sessionId,
            String tokenId,
            Instant expiresAt,
            Instant sessionExpiresAt,
            boolean refreshToken
    ) {
    }
}
