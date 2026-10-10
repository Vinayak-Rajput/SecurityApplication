package com.hdfclife.capstoneone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.hdfclife.capstoneone.auth.dto.*;
import com.hdfclife.capstoneone.auth.service.AuthenticationService.IssuedTokens;
import com.hdfclife.capstoneone.auth.service.AuthenticationService;
import com.hdfclife.capstoneone.auth.dto.SignupResponse;
import com.hdfclife.capstoneone.auth.security.BearerTokenResolver;
import com.hdfclife.capstoneone.auth.security.RefreshRequestOriginValidator;
import com.hdfclife.capstoneone.auth.security.RefreshTokenCookieFactory;
import com.hdfclife.capstoneone.user.service.UserRegistrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private BearerTokenResolver bearerTokenResolver;

    @MockBean
    private UserRegistrationService userRegistrationService;

    @MockBean
    private RefreshRequestOriginValidator refreshRequestOriginValidator;

    @MockBean
    private RefreshTokenCookieFactory refreshTokenCookieFactory;

    private final ResponseCookie mockIssuedCookie = ResponseCookie.from("refreshToken", "mock-refresh-token")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(604800)
            .build();

    private final ResponseCookie mockClearedCookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
            .build();

    @Nested
    @DisplayName("POST /login")
    class LoginTests {

        @Test
        @DisplayName("Should authenticate user, return 200 OK, Set-Cookie header, and access token payload")
        void loginSuccess() throws Exception {
            LoginRequest loginRequest = new LoginRequest("vinayak", "securePassword123");
            IssuedTokens issuedTokens = new IssuedTokens("mock-access-token", "mock-refresh-token");

            given(authenticationService.login(any(LoginRequest.class), any(String.class))).willReturn(issuedTokens);
            given(refreshTokenCookieFactory.issue("mock-refresh-token")).willReturn(mockIssuedCookie);

            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .with(request -> {
                                request.setRemoteAddr("192.168.1.100");
                                return request;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, mockIssuedCookie.toString()))
                    .andExpect(jsonPath("$.message").value("Login successful"))
                    .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));

            verify(authenticationService).login(refEq(loginRequest), eq("192.168.1.100"));
            verify(refreshTokenCookieFactory).issue("mock-refresh-token");
        }
    }

    @Nested
    @DisplayName("POST /signup")
    class SignupTests {

        @Test
        @DisplayName("Should register user and return 201 CREATED with SignupResponse")
        void signupSuccess() throws Exception {
            SignupRequest signupRequest = new SignupRequest("vinayak", "vinayak@example.com", "securePassword123");
            SignupResponse expectedResponse = new SignupResponse("User registered successfully");

            given(userRegistrationService.register(any(SignupRequest.class))).willReturn(expectedResponse);

            mockMvc.perform(post("/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("User registered successfully"));

            verify(userRegistrationService).register(refEq(signupRequest));
        }
    }

    @Nested
    @DisplayName("GET /auth")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should resolve bearer token and return 200 OK with AuthenticationResponse")
        void validateTokenSuccess() throws Exception {
            String rawAuthHeader = "Bearer mock-access-token";
            String resolvedToken = "mock-access-token";
            AuthenticationResponse expectedAuthenticationResponse = new AuthenticationResponse("vinayak", "ROLE_USER",true);

            given(bearerTokenResolver.resolve(rawAuthHeader)).willReturn(resolvedToken);
            given(authenticationService.validateToken(resolvedToken)).willReturn(expectedAuthenticationResponse);

            mockMvc.perform(get("/auth")
                            .header(HttpHeaders.AUTHORIZATION, rawAuthHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("vinayak"))
                    .andExpect(jsonPath("$.role").value("ROLE_USER"));

            verify(bearerTokenResolver).resolve(rawAuthHeader);
            verify(authenticationService).validateToken(resolvedToken);
        }
    }

    @Nested
    @DisplayName("POST /refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should validate origin, refresh tokens, set new cookie, and return 200 OK")
        void refreshSuccess() throws Exception {
            String existingRefreshToken = "old-refresh-token";
            String originHeader = "https://app.example.com";
            IssuedTokens newIssuedTokens = new IssuedTokens("new-access-token", "new-refresh-token");

            doNothing().when(refreshRequestOriginValidator).requireAllowed(originHeader);
            given(authenticationService.refresh(existingRefreshToken)).willReturn(newIssuedTokens);
            given(refreshTokenCookieFactory.issue("new-refresh-token")).willReturn(mockIssuedCookie);

            mockMvc.perform(post("/refresh")
                            .header("Origin", originHeader)
                            .cookie(new Cookie(RefreshTokenCookieFactory.COOKIE_NAME, existingRefreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, mockIssuedCookie.toString()))
                    .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));

            verify(refreshRequestOriginValidator).requireAllowed(originHeader);
            verify(authenticationService).refresh(existingRefreshToken);
            verify(refreshTokenCookieFactory).issue("new-refresh-token");
        }
    }

    @Nested
    @DisplayName("POST /logout")
    class LogoutTests {

        @Test
        @DisplayName("Should resolve token, clear refresh cookie, execute logout, and return 200 OK")
        void logoutSuccess() throws Exception {
            String rawAuthHeader = "Bearer mock-access-token";
            String resolvedToken = "mock-access-token";
            SignupResponse logoutResponse = new SignupResponse("Logout successful");

            given(bearerTokenResolver.resolve(rawAuthHeader)).willReturn(resolvedToken);
            given(refreshTokenCookieFactory.clear()).willReturn(mockClearedCookie);
            given(authenticationService.logout(resolvedToken)).willReturn(logoutResponse);

            mockMvc.perform(post("/logout")
                            .header(HttpHeaders.AUTHORIZATION, rawAuthHeader))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, mockClearedCookie.toString()))
                    .andExpect(jsonPath("$.message").value("Logout successful"));

            verify(bearerTokenResolver).resolve(rawAuthHeader);
            verify(refreshTokenCookieFactory).clear();
            verify(authenticationService).logout(resolvedToken);
        }
    }
}