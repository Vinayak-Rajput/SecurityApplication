package com.hdfclife.capstoneone.util;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

public class Sha256Test {

    @ParameterizedTest
    @CsvSource({
            "'', e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            "'hello world', b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
            "'abc', ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    })
    @DisplayName("Should return correct SHA-256 hex string for standard test vectors")
    void hash_ValidInput_ReturnsExpectedHex(String input, String expectedHash) {
        String actualHash = Sha256.hash(input);
        assertEquals(expectedHash, actualHash);
    }

    @Test
    @DisplayName("Should throw NullPointerException when input is null")
    void hash_NullInput_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Sha256.hash(null));
    }

    @Test
    @DisplayName("Should invoke private constructor for 100% code coverage")
    void privateConstructor_ReflectivelyInvoked_InstantiatesClass() throws Exception {
        Constructor<Sha256> constructor = Sha256.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");

        constructor.setAccessible(true);
        Sha256 instance = constructor.newInstance();
        assertNotNull(instance);
    }
}