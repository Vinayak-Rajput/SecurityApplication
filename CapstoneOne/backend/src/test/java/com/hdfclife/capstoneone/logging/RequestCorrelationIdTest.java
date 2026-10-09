package com.hdfclife.capstoneone.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RequestCorrelationIdTest {

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("ensure() should reuse pre-existing correlation ID stored in request attributes")
    void ensureReusesExistingAttribute() {
        String existingId = UUID.randomUUID().toString();
        request.setAttribute(RequestCorrelationId.REQUEST_ATTRIBUTE, existingId);
        request.addHeader(RequestCorrelationId.HEADER_NAME, UUID.randomUUID().toString()); // Should be ignored

        String result = RequestCorrelationId.ensure(request);

        assertEquals(existingId, result);
        assertEquals(existingId, request.getAttribute(RequestCorrelationId.REQUEST_ATTRIBUTE));
    }

    @Test
    @DisplayName("ensure() should accept valid UUID header and normalize uppercase characters to lower case")
    void ensureAcceptsValidUuidHeaderAndNormalizesCasing() {
        String originalUuid = "123E4567-E89B-12D3-A456-426614174000";
        String expectedLower = originalUuid.toLowerCase(java.util.Locale.ROOT);
        request.addHeader(RequestCorrelationId.HEADER_NAME, originalUuid);

        String result = RequestCorrelationId.ensure(request);

        assertEquals(expectedLower, result);
        assertEquals(expectedLower, request.getAttribute(RequestCorrelationId.REQUEST_ATTRIBUTE));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "invalid-uuid-string",
            "12345",
            "123e4567-e89b-12d3-a456",
            "123e4567-e89b-12d3-a456-426614174000-extra",
            "123e4567e89b12d3a456426614174000" // missing hyphens
    })
    @DisplayName("ensure() should fallback to generating a new random UUID when header is missing or invalid")
    void ensureGeneratesNewUuidOnMissingOrInvalidHeader(String invalidHeader) {
        if (invalidHeader != null) {
            request.addHeader(RequestCorrelationId.HEADER_NAME, invalidHeader);
        }

        String result = RequestCorrelationId.ensure(request);

        assertNotNull(result);
        assertDoesNotThrow(() -> UUID.fromString(result)); // Validates output is a valid UUID
        assertNotEquals(invalidHeader, result);
        assertEquals(result, request.getAttribute(RequestCorrelationId.REQUEST_ATTRIBUTE));
    }

    @Test
    @DisplayName("attach() should set header on response and store ID on request attribute")
    void attachPopulatesResponseHeaderAndRequestAttribute() {
        String validHeader = "123e4567-e89b-12d3-a456-426614174000";
        request.addHeader(RequestCorrelationId.HEADER_NAME, validHeader);

        String result = RequestCorrelationId.attach(request, response);

        assertEquals(validHeader, result);
        assertEquals(validHeader, response.getHeader(RequestCorrelationId.HEADER_NAME));
        assertEquals(validHeader, request.getAttribute(RequestCorrelationId.REQUEST_ATTRIBUTE));
    }

    @Test
    @DisplayName("Private constructor should be non-instantiable via reflection")
    void privateConstructorTest() throws NoSuchMethodException {
        Constructor<RequestCorrelationId> constructor = RequestCorrelationId.class.getDeclaredConstructor();

        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(Exception.class, exception.getCause());
    }
}