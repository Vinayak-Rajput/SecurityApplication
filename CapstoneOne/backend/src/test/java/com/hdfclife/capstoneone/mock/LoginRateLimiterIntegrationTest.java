package com.hdfclife.capstoneone.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "resilience4j.ratelimiter.instances.loginAccount.limitForPeriod=5",
        "resilience4j.ratelimiter.instances.loginAccount.limitRefreshPeriod=1m",
        "resilience4j.ratelimiter.instances.loginAccount.timeoutDuration=0s",
        "resilience4j.ratelimiter.instances.loginClientIp.limitForPeriod=100"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoginRateLimiterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginShouldAllowFiveRequestsAndRejectSixthRequest() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"vinay","password":"Password@123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"vinay","password":"Password@123"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."));
    }
}
