package com.vinayak.secapp.audit.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditEventTest {

    // Mock/Dummy enum value - replace with your actual AuditEventType enum reference
    private static final AuditEventType AUTH_TYPE = AuditEventType.LOGIN_SUCCESS;
    private static final AuditEventType REQUEST_TYPE = AuditEventType.USER_UPDATE;

    @Test
    @DisplayName("authentication factory method should populate auth fields and set HTTP request fields to null")
    void authenticationFactoryMethodSetsDefaults() {
        AuditEvent event = AuditEvent.authentication(
                AUTH_TYPE,
                "alice",
                "User logged in successfully"
        );

        assertEquals(AUTH_TYPE, event.type());
        assertEquals("alice", event.actorUsername());
        assertEquals("User logged in successfully", event.message());

        // HTTP/Request specific fields must default to null
        assertNull(event.subjectUsername());
        assertNull(event.httpMethod());
        assertNull(event.requestPath());
        assertNull(event.httpStatus());
    }

    @Test
    @DisplayName("request factory method should map all parameters correctly")
    void requestFactoryMethodPopulatesAllFields() {
        AuditEvent event = AuditEvent.request(
                REQUEST_TYPE,
                "admin_user",
                "target_user",
                "POST",
                "/api/v1/users",
                201,
                "User created successfully"
        );

        assertEquals(REQUEST_TYPE, event.type());
        assertEquals("admin_user", event.actorUsername());
        assertEquals("target_user", event.subjectUsername());
        assertEquals("POST", event.httpMethod());
        assertEquals("/api/v1/users", event.requestPath());
        assertEquals(201, event.httpStatus());
        assertEquals("User created successfully", event.message());
    }

    @Test
    @DisplayName("Canonical constructor should allow manual instantiation with all fields")
    void canonicalConstructorWorks() {
        AuditEvent event = new AuditEvent(
                AUTH_TYPE,
                "alice",
                "bob",
                "GET",
                "/api/v1/resource",
                200,
                "Direct construct"
        );

        assertEquals(AUTH_TYPE, event.type());
        assertEquals("alice", event.actorUsername());
        assertEquals("bob", event.subjectUsername());
        assertEquals("GET", event.httpMethod());
        assertEquals("/api/v1/resource", event.requestPath());
        assertEquals(200, event.httpStatus());
        assertEquals("Direct construct", event.message());
    }

    @Test
    @DisplayName("Records with identical values should be equal and have matching hashCodes")
    void equalsAndHashCodeContract() {
        AuditEvent event1 = AuditEvent.authentication(AUTH_TYPE, "alice", "Success");
        AuditEvent event2 = AuditEvent.authentication(AUTH_TYPE, "alice", "Success");
        AuditEvent event3 = AuditEvent.authentication(AUTH_TYPE, "bob", "Success");

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
        assertNotEquals(event1, event3);
    }
}