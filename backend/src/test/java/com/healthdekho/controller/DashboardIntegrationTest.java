package com.healthdekho.controller;

import com.healthdekho.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DASH / PROD - dashboard and available products (FR-1B, FR-1C)")
class DashboardIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("DASH-01 customer sees only their own policies")
    void dashboard_showsOwnPoliciesOnly() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(as(AARAV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPolicies").value(1))
                .andExpect(jsonPath("$.policies[0].policyNumber").value("HDP-2026-0001"))
                .andExpect(jsonPath("$.policies[0].policyName").value("HealthDekho Secure Term"))
                .andExpect(jsonPath("$.policies[0].maturityDate").value("2046-11-19"))
                .andExpect(jsonPath("$.policies[0].premiumAmount").value(1250.00))
                .andExpect(jsonPath("$.message").doesNotExist());

        mockMvc.perform(get("/api/dashboard").with(as(MEERA)))
                .andExpect(jsonPath("$.policies[*].policyNumber", contains("HDP-2026-0002")));
    }

    @Test
    @DisplayName("DASH-02 customer without policies gets an empty list and a friendly message")
    void dashboard_noPolicies_showsMessage() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(as(ROHAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPolicies").value(0))
                .andExpect(jsonPath("$.policies", hasSize(0)))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("PROD-01 available products exclude products the customer already holds")
    void availableProducts_excludeOwned() throws Exception {
        mockMvc.perform(get("/api/products/available").with(as(AARAV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].productCode", contains("HD-HEALTH-001", "HD-SAV-001")));

        mockMvc.perform(get("/api/products/available").with(as(ROHAN)))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("PROD-02 inactive products are never offered")
    void availableProducts_excludeInactive() throws Exception {
        jdbc.update("update insurance_product set active = false where product_id = 203");

        mockMvc.perform(get("/api/products/available").with(as(AARAV)))
                .andExpect(jsonPath("$[*].productCode", contains("HD-HEALTH-001")));
    }

    @Test
    @DisplayName("CUST-01 profile endpoint returns the logged-in customer")
    void me_returnsProfile() throws Exception {
        mockMvc.perform(get("/api/customers/me").with(as(MEERA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Meera Iyer"))
                .andExpect(jsonPath("$.dateOfBirth").value("1985-09-22"))
                .andExpect(jsonPath("$.age").value(41));
    }
}
