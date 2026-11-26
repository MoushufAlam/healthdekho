package com.healthdekho.controller;

import com.healthdekho.ai.AiSuggestion;
import com.healthdekho.ai.AiUnavailableException;
import com.healthdekho.ai.RecommendationAiClient;
import com.healthdekho.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AI - recommendations with validation, persistence and fallback (FR-2)")
class RecommendationIntegrationTest extends IntegrationTestBase {

    // the real Gemini client is swapped for a mock: tests must not depend on network or an API key
    @MockitoBean
    private RecommendationAiClient aiClient;

    @BeforeEach
    void modelInfo() {
        when(aiClient.modelName()).thenReturn("gemini-test");
        when(aiClient.modelVersion()).thenReturn("mock");
    }

    private String request(Map<String, Object> body) throws Exception {
        return json(body);
    }

    @Test
    @DisplayName("AI-01 AI answer is filtered to real, eligible, not-owned products and saved")
    void aiAnswer_isValidatedAndPersisted() throws Exception {
        when(aiClient.suggest(any())).thenReturn(List.of(
                new AiSuggestion("HD-SAV-001", 1, "Builds savings for retirement", "high"),
                new AiSuggestion("FAKE-999", 2, "Invented product", "HIGH"),
                new AiSuggestion("HD-TERM-001", 3, "Already owned by the customer", "MEDIUM"),
                new AiSuggestion("hd-health-001", 4, "Covers hospital bills", "unknown")));

        String body = mockMvc.perform(post("/api/recommendations").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(Map.of("insuranceGoals", "Retirement savings", "preferredTerm", "10 years"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("AI"))
                .andExpect(jsonPath("$.calculatedAge").value(36))
                .andExpect(jsonPath("$.modelName").value("gemini-test"))
                .andExpect(jsonPath("$.recommendations", hasSize(2)))
                .andExpect(jsonPath("$.recommendations[0].productCode").value("HD-SAV-001"))
                .andExpect(jsonPath("$.recommendations[0].rank").value(1))
                .andExpect(jsonPath("$.recommendations[0].suitability").value("HIGH"))
                .andExpect(jsonPath("$.recommendations[1].productCode").value("HD-HEALTH-001"))
                .andExpect(jsonPath("$.recommendations[1].suitability").value("MEDIUM"))
                .andReturn().getResponse().getContentAsString();

        long requestId = objectMapper.readTree(body).get("recommendationRequestId").asLong();
        Map<String, Object> saved = jdbc.queryForMap(
                "select model_name, prompt_version, calculated_age, insurance_goals from ai_recommendation_request "
                        + "where recommendation_request_id = ?", requestId);
        assertThat(saved).containsEntry("model_name", "gemini-test")
                .containsEntry("prompt_version", "rec-v1")
                .containsEntry("calculated_age", 36)
                .containsEntry("insurance_goals", "Retirement savings");
        Long items = jdbc.queryForObject(
                "select count(*) from ai_recommendation_item where recommendation_request_id = ?", Long.class, requestId);
        assertThat(items).isEqualTo(2);
    }

    @Test
    @DisplayName("AI-02 AI failure (timeout) falls back to rule-based recommendations, still saved")
    void aiFailure_fallsBack() throws Exception {
        when(aiClient.suggest(any()))
                .thenThrow(new AiUnavailableException("AI did not answer within 20s", new TimeoutException()));

        String body = mockMvc.perform(post("/api/recommendations").with(as(ROHAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(Map.of("insuranceGoals", "Health coverage for parents",
                                "preferredPolicyType", "HEALTH"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("FALLBACK"))
                .andExpect(jsonPath("$.modelName").value("rule-based-fallback"))
                .andExpect(jsonPath("$.message", containsString("took too long")))
                .andExpect(jsonPath("$.recommendations[0].productCode").value("HD-HEALTH-001"))
                .andExpect(jsonPath("$.recommendations[0].suitability").value("HIGH"))
                .andReturn().getResponse().getContentAsString();

        long requestId = objectMapper.readTree(body).get("recommendationRequestId").asLong();
        String model = jdbc.queryForObject(
                "select model_name from ai_recommendation_request where recommendation_request_id = ?",
                String.class, requestId);
        assertThat(model).isEqualTo("rule-based-fallback");
    }

    @Test
    @DisplayName("AI-03 AI answer with no usable products also falls back")
    void aiUselessAnswer_fallsBack() throws Exception {
        when(aiClient.suggest(any())).thenReturn(List.of(new AiSuggestion("NOPE-1", 1, "x", "HIGH")));

        mockMvc.perform(post("/api/recommendations").with(as(ROHAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(Map.of("preferredPolicyType", "SAVINGS"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("FALLBACK"))
                .andExpect(jsonPath("$.recommendations[0].productCode").value("HD-SAV-001"));
    }

    @Test
    @DisplayName("AI-04 age from the entered DOB excludes products outside their entry ages; AI not called if none")
    void noEligibleProducts_noAiCall() throws Exception {
        mockMvc.perform(post("/api/recommendations").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(Map.of("dateOfBirth", "1950-01-01"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.calculatedAge").value(76))
                .andExpect(jsonPath("$.recommendations", hasSize(0)))
                .andExpect(jsonPath("$.message", containsString("No products are available for your age (76)")));

        verify(aiClient, never()).suggest(any());
    }

    @Test
    @DisplayName("AI-05 empty request or future date of birth is rejected")
    void invalidInput_400() throws Exception {
        mockMvc.perform(post("/api/recommendations").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/recommendations").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(Map.of("dateOfBirth", "2030-01-01"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Date of birth cannot be in the future"));
    }
}
