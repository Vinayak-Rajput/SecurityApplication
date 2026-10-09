package com.hdfclife.capstoneone.security;

import com.hdfclife.capstoneone.auth.exception.InvalidAuthorizationHeaderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BearerTokenResolverTest {

    private final BearerTokenResolver resolver = new BearerTokenResolver();

    @Test
    void resolveExtractsBearerToken() {
        assertEquals("abc.def.ghi", resolver.resolve("Bearer abc.def.ghi"));
    }

    @Test
    void resolveRejectsMissingMalformedOrEmptyBearerValues() {
        assertThrows(InvalidAuthorizationHeaderException.class, () -> resolver.resolve(null));
        assertThrows(InvalidAuthorizationHeaderException.class, () -> resolver.resolve("Basic abc"));
        assertThrows(InvalidAuthorizationHeaderException.class, () -> resolver.resolve("Bearer "));
        assertThrows(InvalidAuthorizationHeaderException.class, () -> resolver.resolve("Bearer"));
    }
}
