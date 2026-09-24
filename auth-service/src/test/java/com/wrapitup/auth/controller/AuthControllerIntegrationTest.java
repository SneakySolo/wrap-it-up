package com.wrapitup.auth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // No special setup needed for basic integration tests
    }

    @Test
    @DisplayName("Should start OAuth login for GET /auth/spotify")
    void testLoginEndpoint() throws Exception {
        mockMvc.perform(get("/auth/spotify"))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("http://localhost/oauth2/authorization/spotify"));
    }

    @Test
    @DisplayName("Health check endpoint should be accessible")
    void testHealthCheckEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Callback endpoint requires authentication")
    void testCallbackRequiresAuth() throws Exception {
        // Without OAuth2 token, this should fail
        // In real scenario, you would mock OAuth2 context
        mockMvc.perform(get("/auth/spotify/callback"))
                .andDo(print())
                // Expected: redirect to login or 401/403
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Should be either redirect (3xx) or unauthorized (4xx)
                    assert (status >= 300 && status < 400) || (status >= 400 && status < 500);
                });
    }

    @Test
    @DisplayName("GET /auth/me requires authentication")
    void testGetMeRequiresAuth() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andDo(print())
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Should be either redirect (3xx) or unauthorized (4xx)
                    assert (status >= 300 && status < 400) || (status >= 400 && status < 500);
                });
    }

    @Test
    @DisplayName("Logout endpoint should be accessible")
    void testLogoutEndpoint() throws Exception {
        mockMvc.perform(get("/auth/logout"))
                .andDo(print())
                // Logout can succeed or redirect depending on Spring Security config
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status >= 200 && status < 400;
                });
    }
}
