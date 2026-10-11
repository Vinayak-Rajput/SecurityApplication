package com.vinayak.secapp.audit.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuditEventTypeTest {

    @ParameterizedTest
    @EnumSource(AuditEventType.class)
    @DisplayName("fromCode should successfully map every valid enum name to its Optional representation")
    void fromCodeValidEnumNames(AuditEventType type) {
        Optional<AuditEventType> result = AuditEventType.fromCode(type.name());

        assertTrue(result.isPresent());
        assertEquals(type, result.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID_EVENT", "login_success", "LOGIN_SUCCESS ", "UNKNOWN"})
    @DisplayName("fromCode should return Optional.empty() for unrecognized, improperly cased, or padded strings")
    void fromCodeInvalidCodes(String invalidCode) {
        Optional<AuditEventType> result = AuditEventType.fromCode(invalidCode);

        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("fromCode should return Optional.empty() when passed null or empty string")
    void fromCodeNullAndEmptyStrings(String nullOrEmpty) {
        Optional<AuditEventType> result = AuditEventType.fromCode(nullOrEmpty);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Ensure total enum count matches expected security and operational event catalog")
    void verifyEnumCount() {
        assertEquals(24, AuditEventType.values().length);
    }
}