package com.vinayak.secapp.audit.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class AuditEventJsonTest {

    @Autowired
    private JacksonTester<AuditEvent> json;

    @Test
    @DisplayName("Should serialize AuditEvent to expected JSON payload")
    void serializesToJson() throws Exception {
        AuditEvent event = AuditEvent.authentication(AuditEventType.LOGIN_SUCCESS, "alice", "User logged in");

        JsonContent<AuditEvent> result = json.write(event);

        assertThat(result).extractingJsonPathStringValue("$.type").isEqualTo("LOGIN_SUCCESS");
        assertThat(result).extractingJsonPathStringValue("$.actorUsername").isEqualTo("alice");
        assertThat(result).extractingJsonPathStringValue("$.message").isEqualTo("User logged in");
        assertThat(result).extractingJsonPathStringValue("$.httpMethod").isNull();
    }
}