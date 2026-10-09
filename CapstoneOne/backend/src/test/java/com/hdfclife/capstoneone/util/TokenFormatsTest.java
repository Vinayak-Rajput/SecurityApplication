package com.hdfclife.capstoneone.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class TokenFormatsTest {

    @Nested
    @DisplayName("isTokenId() Tests")
    class IsTokenIdTests {

        @Test
        @DisplayName("Should return true for a valid canonical UUID v4 string")
        void isTokenId_ValidUuid_ReturnsTrue() {
            String validUuid = "123e4567-e89b-12d3-a456-426614174000";
            assertTrue(TokenFormats.isTokenId(validUuid));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "123E4567-E89B-12D3-A456-426614174000", // Uppercase (fails .equals due to canonical lowercase toString)
            "123e4567-e89b-12d3-a456",             // Too short / missing group
            "123e4567e89b12d3a456426614174000",     // Missing hyphens
            "invalid-uuid-string-format",          // Arbitrary string
            "g23e4567-e89b-12d3-a456-426614174000"  // Contains non-hex character 'g'
        })
        @NullAndEmptySource
        @DisplayName("Should return false for invalid, non-canonical, or null/empty UUIDs")
        void isTokenId_InvalidInput_ReturnsFalse(String input) {
            assertFalse(TokenFormats.isTokenId(input));
        }
    }

    @Nested
    @DisplayName("isSha256() Tests")
    class IsSha256Tests {

        @Test
        @DisplayName("Should return true for valid 64-character lowercase SHA-256 hex string")
        void isSha256_ValidSha256_ReturnsTrue() {
            String validSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            assertTrue(TokenFormats.isSha256(validSha256));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855", // Uppercase hex (regex requires lowercase)
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85",  // 63 characters (too short)
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b8555", // 65 characters (too long)
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85g"  // Contains non-hex character 'g'
        })
        @NullAndEmptySource
        @DisplayName("Should return false for invalid length, uppercase, or non-hex strings")
        void isSha256_InvalidInput_ReturnsFalse(String input) {
            assertFalse(TokenFormats.isSha256(input));
        }
    }

    @Test
    @DisplayName("Should invoke private constructor for 100% code coverage")
    void privateConstructor_ReflectivelyInvoked_InstantiatesClass() throws Exception {
        Constructor<TokenFormats> constructor = TokenFormats.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");

        constructor.setAccessible(true);
        TokenFormats instance = constructor.newInstance();
        assertNotNull(instance);
    }
}