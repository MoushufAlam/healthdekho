package com.healthdekho.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdekho.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AUTH - login and JWT protection (FR-1A)")
class AuthIntegrationTest extends IntegrationTestBase {

    private static final String PASSWORD = "Password@123";

    @Test
    @DisplayName("AUTH-01 valid credentials return a JWT that unlocks the dashboard")
    void login_success_tokenWorksOnProtectedEndpoint() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "Aarav.Sharma@example.com", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.customer.customerId").value(1001))
                .andExpect(jsonPath("$.customer.age").value(36))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(body).get("accessToken").asText();

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Aarav Sharma"));
    }

    @Test
    @DisplayName("AUTH-02 wrong password returns 401 with a generic message")
    void login_wrongPassword_401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "aarav.sharma@example.com", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("AUTH-03 unknown email gets the same 401 message as a wrong password")
    void login_unknownEmail_401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nobody@example.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("AUTH-04 blank/invalid fields return 400 with field errors")
    void login_invalidBody_400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "not-an-email", "password", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("AUTH-05 locked customer cannot log in")
    void login_lockedCustomer_401() throws Exception {
        jdbc.update("update customer set status = 'LOCKED' where customer_id = 1003");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "rohan.verma@example.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("This account is not active. Please contact support"));
    }

    @Test
    @DisplayName("AUTH-06 protected endpoint without a token returns 401 JSON")
    void protectedEndpoint_noToken_401() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("AUTH-07 forged or broken token returns 401")
    void protectedEndpoint_badToken_401() throws Exception {
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token. Please log in again"));
    }

    @Test
    @DisplayName("AUTH-08 health check stays public")
    void health_isPublic() throws Exception {
        JsonNode health = objectMapper.readTree(mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(health.get("status").asText()).isEqualTo("UP");
    }
}
