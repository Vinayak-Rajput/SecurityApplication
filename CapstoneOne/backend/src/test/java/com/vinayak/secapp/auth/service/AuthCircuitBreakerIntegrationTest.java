package com.vinayak.secapp.auth.service;

import com.vinayak.secapp.auth.service.TokenService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "resilience4j.ratelimiter.instances.loginAccount.limitForPeriod=100",
        "resilience4j.ratelimiter.instances.loginClientIp.limitForPeriod=100"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthCircuitBreakerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @SpyBean
    private TokenService tokenService;

    @Test
    void authenticatedRequestsRemainAvailableWhenDatabaseCallsSucceed() throws Exception {
        String token = loginAndGetToken();

        for (int i = 0; i < 6; i++) {
            mockMvc.perform(get("/auth").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authenticated").value(true));
        }

        assertEquals(CircuitBreaker.State.CLOSED,
                circuitBreakerRegistry.circuitBreaker("authenticationDatabase").getState());
    }

    @Test
    void datasourceFailuresDuringAuthValidationOpenCircuitAndReturn503() throws Exception {
        String token = loginAndGetToken();
        doThrow(new DataAccessResourceFailureException("session store unavailable"))
                .when(tokenService).isAccessTokenActive(any(UUID.class), anyString());

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/auth").header("Authorization", "Bearer " + token))
                    .andExpect(status().isServiceUnavailable());
        }

        assertEquals(CircuitBreaker.State.OPEN,
                circuitBreakerRegistry.circuitBreaker("authenticationDatabase").getState());

        mockMvc.perform(get("/auth").header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(
                        "Authentication data is temporarily unavailable. Please try again later."));

        assertEquals(CircuitBreaker.State.OPEN,
                circuitBreakerRegistry.circuitBreaker("authenticationDatabase").getState());
    }

    private String loginAndGetToken() throws Exception {
        String body = mockMvc.perform(post("/login")
                        .contentType("application/json")
                        .content("""
                                {"username":"vinay","password":"Password@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int start = body.indexOf("\"token\":\"") + 9;
        int end = body.indexOf('"', start);
        return body.substring(start, end);
    }
}
