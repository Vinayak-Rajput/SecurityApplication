package com.vinayak.secapp.common.config;

import com.vinayak.secapp.common.config.properties.FrontendProperties;
import com.vinayak.secapp.common.security.SecurityExceptionHandler;
import com.vinayak.secapp.auth.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@Import({SecurityConfig.class, SecurityConfigTest.TestConfig.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private SecurityExceptionHandler securityExceptionHandler;

    @MockBean
    private FrontendProperties frontendProperties;

    @MockBean
    @TestConfiguration
    static class TestConfig {
        // Minimal mock controller to test security path rules against 200 OK responses
    }

    @RestController
    static class TestController {
        @PostMapping("/login")
        public ResponseEntity<String> login() { return ResponseEntity.ok("login"); }

        @PostMapping("/signup")
        public ResponseEntity<String> signup() { return ResponseEntity.ok("signup"); }

        @PostMapping("/logout")
        public ResponseEntity<String> logout() { return ResponseEntity.ok("logout"); }

        @GetMapping("/auth")
        public ResponseEntity<String> auth() { return ResponseEntity.ok("auth"); }

        @GetMapping("/users/me")
        public ResponseEntity<String> userMe() { return ResponseEntity.ok("me"); }

        @GetMapping("/users")
        public ResponseEntity<String> getUsers() { return ResponseEntity.ok("users"); }

        @GetMapping("/users/{id}")
        public ResponseEntity<String> getUserById(@PathVariable String id) { return ResponseEntity.ok("user"); }

        @PostMapping("/users")
        public ResponseEntity<String> createUser() { return ResponseEntity.ok("created"); }

        @DeleteMapping("/users/{id}")
        public ResponseEntity<String> deleteUser(@PathVariable String id) { return ResponseEntity.ok("deleted"); }

        @GetMapping("/audit/logs")
        public ResponseEntity<String> auditLogs() { return ResponseEntity.ok("logs"); }

        @GetMapping("/audit/other")
        public ResponseEntity<String> auditOther() { return ResponseEntity.ok("other"); }

        @GetMapping("/unknown-route")
        public ResponseEntity<String> unknownRoute() { return ResponseEntity.ok("unknown"); }
    }

    @Nested
    @DisplayName("Endpoint Authorization Rules")
    class AuthorizationTests {

        @Test
        @DisplayName("POST /login & POST /refresh permit unauthenticated access")
        void permitAllPublicEndpoints() throws Exception {
            mockMvc.perform(post("/login"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("POST /signup allows anonymous users")
        void allowSignupForAnonymous() throws Exception {
            mockMvc.perform(post("/signup"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("POST /signup denies authenticated users (anonymous-only)")
        void denySignupForAuthenticatedUser() throws Exception {
            mockMvc.perform(post("/signup"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /users/me allows ROLE_USER")
        void allowUserRoleForUserMe() throws Exception {
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /users denies ROLE_USER")
        void denyUserRoleForAdminEndpoint() throws Exception {
            mockMvc.perform(get("/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /users, POST /users, DELETE /users/{id} allow ROLE_ADMIN")
        void allowAdminRoleForAdminEndpoints() throws Exception {
            mockMvc.perform(get("/users")).andExpect(status().isOk());
            mockMvc.perform(post("/users")).andExpect(status().isOk());
            mockMvc.perform(delete("/users/123")).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "AUDITOR")
        @DisplayName("GET /audit/logs allows ROLE_AUDITOR")
        void allowAuditorRoleForAuditLogs() throws Exception {
            mockMvc.perform(get("/audit/logs"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "AUDITOR")
        @DisplayName("Fallback denyAll() blocks unmapped routes under /audit/** and general routes")
        void denyUnmappedAuditAndGeneralRoutes() throws Exception {
            mockMvc.perform(get("/audit/other"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/unknown-route"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("CORS Configuration")
    class CorsTests {

        @Test
        @DisplayName("Preflight OPTIONS request returns configured CORS headers")
        void corsPreflightConfiguredCorrectly() throws Exception {
            given(frontendProperties.getAllowedOrigins())
                    .willReturn(List.of("https://app.example.com"));

            mockMvc.perform(options("/login")
                            .header(HttpHeaders.ORIGIN, "https://app.example.com")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example.com"))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
        }
    }

    @Nested
    @DisplayName("Filter & Bean Lifecycle Configuration")
    class FilterRegistrationTests {

        @Test
        @Test
        @DisplayName("JwtAuthenticationFilter bean registration disables automatic Servlet filter chaining")
        @SuppressWarnings("unchecked")
        void jwtAuthenticationFilterAutoRegistrationIsDisabled() {
            FilterRegistrationBean<JwtAuthenticationFilter> registration =
                    applicationContext.getBean("jwtAuthenticationFilterRegistration", FilterRegistrationBean.class);

            assertThat(registration).isNotNull();
            assertThat(registration.isEnabled()).isFalse();
        }
    }
}