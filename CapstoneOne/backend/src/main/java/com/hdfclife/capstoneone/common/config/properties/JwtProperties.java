package com.hdfclife.capstoneone.common.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull @DurationUnit(ChronoUnit.MILLIS) Duration expiration,
        @NotNull @DurationUnit(ChronoUnit.MILLIS) Duration refreshExpiration,
        @NotNull @DurationUnit(ChronoUnit.MILLIS) Duration refreshMaxLifetime,
        @NotBlank String issuer,
        @NotBlank String accessAudience,
        @NotBlank String refreshAudience,
        @NotBlank String activeKeyId,
        String previousKeyId,
        String previousSecret
) {

    @AssertTrue(message = "JWT signing secret must be Base64-encoded and contain at least 32 random bytes")
    public boolean isSigningKeyStrongEnough() {
        return hasStrongKey(secret);
    }

    @AssertTrue(message = "Previous JWT key ID and secret must both be configured and use a distinct ID")
    public boolean isPreviousSigningKeyValid() {
        if (previousSecret == null || previousSecret.isBlank()) {
            return previousKeyId == null || previousKeyId.isBlank();
        }
        return previousKeyId != null && !previousKeyId.isBlank()
                && !previousKeyId.equals(activeKeyId)
                && hasStrongKey(previousSecret);
    }

    @AssertTrue(message = "JWT lifetimes must be positive")
    public boolean areLifetimesPositive() {
        return isPositive(expiration) && isPositive(refreshExpiration) && isPositive(refreshMaxLifetime);
    }

    @AssertTrue(message = "JWT maximum refresh lifetime must not be shorter than refresh expiration")
    public boolean isMaximumRefreshLifetimeValid() {
        return refreshExpiration != null && refreshMaxLifetime != null
                && refreshMaxLifetime.compareTo(refreshExpiration) >= 0;
    }

    private static boolean isPositive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }

    public byte[] signingKey(String keyId) {
        // Only active and explicitly configured previous keys are accepted during a key-rotation window.
        if (activeKeyId.equals(keyId)) {
            return Base64.getDecoder().decode(secret);
        }
        if (previousKeyId != null && previousKeyId.equals(keyId)
                && previousSecret != null && !previousSecret.isBlank()) {
            return Base64.getDecoder().decode(previousSecret);
        }
        throw new IllegalArgumentException("Unrecognized JWT signing key ID");
    }

    private static boolean hasStrongKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            return false;
        }
        try {
            return Base64.getDecoder().decode(encodedKey).length >= 32;
        }
        catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
