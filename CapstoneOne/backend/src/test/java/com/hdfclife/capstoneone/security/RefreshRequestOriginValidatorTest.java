package com.hdfclife.capstoneone.security;

import com.hdfclife.capstoneone.common.config.properties.FrontendProperties;
import com.hdfclife.capstoneone.auth.exception.DisallowedRequestOriginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshRequestOriginValidatorTest {

    @Mock
    private FrontendProperties frontendProperties;

    private RefreshRequestOriginValidator validator;

    private static final String ALLOWED_ORIGIN_1 = "https://app.example.com";
    private static final String ALLOWED_ORIGIN_2 = "https://admin.example.com";

    @BeforeEach
    void setUp() {
        when(frontendProperties.getAllowedOrigins())
                .thenReturn(List.of(ALLOWED_ORIGIN_1, ALLOWED_ORIGIN_2));

        validator = new RefreshRequestOriginValidator(frontendProperties);
    }

    @Test
    @DisplayName("Should pass silently when origin is in allowed origins list")
    void requireAllowed_AllowedOrigin_DoesNotThrowException() {
        assertDoesNotThrow(() -> validator.requireAllowed(ALLOWED_ORIGIN_1));
        assertDoesNotThrow(() -> validator.requireAllowed(ALLOWED_ORIGIN_2));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://malicious.com",
        "https://APP.EXAMPLE.COM", // Case sensitivity check
        "https://app.example.com/", // Trailing slash difference
        "http://app.example.com"    // Protocol mismatch (HTTP vs HTTPS)
    })
    @NullAndEmptySource
    @DisplayName("Should throw DisallowedRequestOriginException for invalid, unlisted, or null/empty origins")
    void requireAllowed_DisallowedOrigin_ThrowsException(String invalidOrigin) {
        DisallowedRequestOriginException exception = assertThrows(
                DisallowedRequestOriginException.class,
                () -> validator.requireAllowed(invalidOrigin)
        );

        assertEquals("Refresh request origin is not allowed", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject all origins when allowed origins configuration is empty")
    void requireAllowed_EmptyAllowedOrigins_RejectsAll() {
        // Arrange
        when(frontendProperties.getAllowedOrigins()).thenReturn(List.of());
        RefreshRequestOriginValidator emptyValidator = new RefreshRequestOriginValidator(frontendProperties);

        // Act & Assert
        assertThrows(
                DisallowedRequestOriginException.class,
                () -> emptyValidator.requireAllowed(ALLOWED_ORIGIN_1)
        );
    }
}