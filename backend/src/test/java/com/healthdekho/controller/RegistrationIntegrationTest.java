package com.healthdekho.controller;

import com.healthdekho.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("REG - policy registration (FR-3)")
class RegistrationIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("REG-01 preview pre-fills customer + product data and calculates dates and totals")
    void preview_calculatesEverything() throws Exception {
        mockMvc.perform(get("/api/policies/preview").param("productId", "203").with(as(AARAV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.fullName").value("Aarav Sharma"))
                .andExpect(jsonPath("$.product.policyName").value("HealthDekho Smart Savings"))
                .andExpect(jsonPath("$.startDate").value("2026-11-26"))
                .andExpect(jsonPath("$.maturityDate").value("2036-11-25"))
                .andExpect(jsonPath("$.numberOfInstallments").value(40))
                .andExpect(jsonPath("$.totalPremiumPayable").value(300000.00))
                .andExpect(jsonPath("$.eligible").value(true));
    }

    @Test
    @DisplayName("REG-02 preview flags a product the customer already holds")
    void preview_alreadyHeld_notEligible() throws Exception {
        mockMvc.perform(get("/api/policies/preview").param("productId", "201").with(as(AARAV)))
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.eligibilityMessage", containsString("already have")));
    }

    @Test
    @DisplayName("REG-03 registration saves policy + full installment schedule and returns the policy number")
    void register_success() throws Exception {
        mockMvc.perform(post("/api/policies").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", 203))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/policies/HDP-2026-0003"))
                .andExpect(jsonPath("$.policyNumber").value("HDP-2026-0003"))
                .andExpect(jsonPath("$.message").value("Policy HDP-2026-0003 registered successfully"))
                .andExpect(jsonPath("$.numberOfInstallments").value(40))
                .andExpect(jsonPath("$.totalPremiumPayable").value(300000.00))
                .andExpect(jsonPath("$.policy.maturityDate").value("2036-11-25"));

        Long installments = jdbc.queryForObject("""
                select count(*) from premium_installment i join customer_policy p on p.policy_id = i.policy_id
                where p.policy_number = 'HDP-2026-0003'""", Long.class);
        assertThat(installments).isEqualTo(40);

        String secondDue = jdbc.queryForObject("""
                select i.due_date || '/' || i.grace_period_end_date from premium_installment i
                join customer_policy p on p.policy_id = i.policy_id
                where p.policy_number = 'HDP-2026-0003' and i.installment_number = 2""", String.class);
        assertThat(secondDue).isEqualTo("2027-02-26/2027-03-26");

        mockMvc.perform(get("/api/products/available").with(as(AARAV)))
                .andExpect(jsonPath("$[*].productCode", org.hamcrest.Matchers.contains("HD-HEALTH-001")));
    }

    @Test
    @DisplayName("REG-04 registering the same product twice is rejected with 409")
    void register_duplicate_409() throws Exception {
        mockMvc.perform(post("/api/policies").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", 201))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already have a policy")));
    }

    @Test
    @DisplayName("REG-05 start date in the past is rejected")
    void register_pastStartDate_400() throws Exception {
        mockMvc.perform(post("/api/policies").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", 203, "startDate", "2026-11-01"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Policy start date cannot be in the past"));
    }

    @Test
    @DisplayName("REG-06 customer outside the product's entry age range is rejected")
    void register_ageNotEligible_400() throws Exception {
        jdbc.update("update insurance_product set min_entry_age = 40 where product_id = 203");

        mockMvc.perform(post("/api/policies").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", 203))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("outside the entry age range 40-55")));
    }

    @Test
    @DisplayName("REG-07 unknown product returns 404, missing product returns 400")
    void register_badProduct() throws Exception {
        mockMvc.perform(post("/api/policies").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", 999))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/policies").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.productId").value("Product is required"));
    }

    @Test
    @DisplayName("REG-08 registering from an AI recommendation marks it as selected")
    void register_fromRecommendation_marksSelected() throws Exception {
        mockMvc.perform(post("/api/policies").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", 203, "recommendationItemId", 7002))))
                .andExpect(status().isCreated());
        flush();

        Boolean selected = jdbc.queryForObject(
                "select selected_for_registration from ai_recommendation_item where recommendation_item_id = 7002",
                Boolean.class);
        assertThat(selected).isTrue();
    }

    @Test
    @DisplayName("REG-09 another customer's recommendation can't be used (404)")
    void register_othersRecommendation_404() throws Exception {
        mockMvc.perform(post("/api/policies").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", 202, "recommendationItemId", 7003))))
                .andExpect(status().isNotFound());
    }
}
