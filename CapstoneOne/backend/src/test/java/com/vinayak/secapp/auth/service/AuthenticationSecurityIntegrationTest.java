package com.vinayak.secapp.auth.service;

import com.vinayak.secapp.auth.entity.Authentication;
import com.vinayak.secapp.auth.repository.AuthenticationSessionRepository;
import com.vinayak.secapp.common.util.Sha256;

import com.vinayak.secapp.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "resilience4j.ratelimiter.instances.loginAccount.limitForPeriod=100",
        "resilience4j.ratelimiter.instances.loginClientIp.limitForPeriod=100"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthenticationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationSessionRepository sessionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginPersistsOnlyTokenIdentifierAndRefreshTokenHash() throws Exception {
        MvcResult loginResult = login();
        JsonNode login = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = login.get("token").asText();
        Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");
        String refreshToken = refreshCookie.getValue();
        UUID sessionId = jwtService.extractSessionId(accessToken);

        assertTrue(refreshCookie.isHttpOnly());
        assertEquals("/refresh", refreshCookie.getPath());
        assertTrue(login.get("refreshToken") == null);

        Authentication session = sessionRepository.findById(sessionId).orElseThrow();

        assertEquals(jwtService.extractTokenId(accessToken), session.getAccessTokenId());
        assertNotEquals(accessToken, session.getAccessTokenId());
        assertEquals(Sha256.hash(refreshToken), session.getRefreshTokenHash());
        assertNotEquals(refreshToken, session.getRefreshTokenHash());
    }

    @Test
    void refreshRotatesTokensAndReplayRevokesTheSession() throws Exception {
        MvcResult loginResult = login();
        JsonNode initialLogin = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String oldAccessToken = initialLogin.get("token").asText();
        Cookie oldRefreshCookie = loginResult.getResponse().getCookie("refresh_token");

        MvcResult refresh = mockMvc.perform(post("/refresh")
                        .cookie(oldRefreshCookie)
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rotated = objectMapper.readTree(refresh.getResponse().getContentAsString());
        String newAccessToken = rotated.get("token").asText();
        Cookie newRefreshCookie = refresh.getResponse().getCookie("refresh_token");
        assertNotEquals(oldAccessToken, newAccessToken);
        assertNotEquals(oldRefreshCookie.getValue(), newRefreshCookie.getValue());
        assertTrue(rotated.get("refreshToken") == null);

        mockMvc.perform(get("/auth").header("Authorization", "Bearer " + oldAccessToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/auth").header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/refresh")
                        .cookie(oldRefreshCookie)
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/auth").header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheSessionImmediately() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isUnauthorized());

        JsonNode response = objectMapper.readTree(login().getResponse().getContentAsString());
        String accessToken = response.get("token").asText();

        MvcResult logout = mockMvc.perform(post("/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(logout.getResponse().getHeader("Set-Cookie").contains("Max-Age=0"));
        mockMvc.perform(get("/auth").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRejectsMissingOrUntrustedOrigin() throws Exception {
        Cookie refreshCookie = login().getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/refresh").cookie(refreshCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/refresh")
                        .cookie(refreshCookie)
                        .header("Origin", "https://attacker.example"))
                .andExpect(status().isForbidden());
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"vinay","password":"Password@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
    }
}
