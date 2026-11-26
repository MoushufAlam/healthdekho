// ============================================================================
// OPTIONAL: FR-5 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: SRCH-01..07 tests, incl. SRCH-02 data isolation (another customer's policy -> 404).
// Only FR-5 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-5" (search the project for it). Theory and flow: docs/steps/step8.readme.md
// ============================================================================
package com.healthdekho.controller;

import com.healthdekho.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SRCH - customer policy search (FR-5)")
class SearchIntegrationTest extends IntegrationTestBase {

    private static final String SEARCH = "/api/policies/search";

    @Test
    @DisplayName("SRCH-01 search by policy number (case-insensitive) returns the policy with payment totals")
    void byPolicyNumber() throws Exception {
        mockMvc.perform(get(SEARCH).param("policyNumber", "hdp-2026-0001").with(as(AARAV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customerName").value("Aarav Sharma"))
                .andExpect(jsonPath("$.content[0].dateOfBirth").value("1990-04-15"))
                .andExpect(jsonPath("$.content[0].totalPremiumPaid").value(1750.00))
                .andExpect(jsonPath("$.content[0].remainingPremium").value(298250.00))
                .andExpect(jsonPath("$.content[0].premiumPaymentStatus").value("ON_TRACK"));
    }

    @Test
    @DisplayName("SRCH-02 ISOLATION: another customer's policy number returns 404")
    void otherCustomersPolicy_404() throws Exception {
        mockMvc.perform(get(SEARCH).param("policyNumber", "HDP-2026-0002").with(as(AARAV)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Policy HDP-2026-0002 not found"));

        mockMvc.perform(get(SEARCH).param("policyNumber", "HDP-2026-0002")
                        .param("dateOfBirth", "1985-09-22").with(as(AARAV)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SRCH-03 policy number + date of birth must both match")
    void byPolicyNumberAndDob() throws Exception {
        mockMvc.perform(get(SEARCH).param("policyNumber", "HDP-2026-0001")
                        .param("dateOfBirth", "1990-04-15").with(as(AARAV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(get(SEARCH).param("policyNumber", "HDP-2026-0001")
                        .param("dateOfBirth", "1990-04-16").with(as(AARAV)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SRCH-04 search by (partial) customer name; other customers' names find nothing")
    void byCustomerName() throws Exception {
        mockMvc.perform(get(SEARCH).param("customerName", "sharma").with(as(AARAV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].policyNumber").value("HDP-2026-0001"));

        mockMvc.perform(get(SEARCH).param("customerName", "Meera").with(as(AARAV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.message").value("No policies match the search criteria"));
    }

    @Test
    @DisplayName("SRCH-05 results are paginated")
    void pagination() throws Exception {
        for (int productId : new int[] {202, 203}) {
            mockMvc.perform(post("/api/policies").with(as(AARAV))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("productId", productId))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get(SEARCH).param("customerName", "Aarav").param("size", "2").with(as(AARAV)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(get(SEARCH).param("customerName", "Aarav").param("size", "2").param("page", "1")
                        .with(as(AARAV)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("SRCH-06 an installment past its grace period makes the policy OVERDUE")
    void overdueStatus() throws Exception {
        jdbc.update("""
                update premium_installment set due_date = '2026-10-01', grace_period_end_date = '2026-11-01'
                where installment_id = 4002""");

        mockMvc.perform(get(SEARCH).param("policyNumber", "HDP-2026-0001").with(as(AARAV)))
                .andExpect(jsonPath("$.content[0].premiumPaymentStatus").value("OVERDUE"));
    }

    @Test
    @DisplayName("SRCH-07 invalid search criteria are rejected before querying")
    void invalidCriteria_400() throws Exception {
        mockMvc.perform(get(SEARCH).with(as(AARAV)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(SEARCH).param("dateOfBirth", "1990-04-15").with(as(AARAV)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Date of birth can only be used together with a policy number"));
        mockMvc.perform(get(SEARCH).param("policyNumber", "HDP-2026-0001").param("customerName", "Aarav")
                        .with(as(AARAV)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(SEARCH).param("customerName", "%").with(as(AARAV)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(SEARCH).param("customerName", "Aarav").param("size", "51").with(as(AARAV)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.size").exists());
        mockMvc.perform(get(SEARCH).param("policyNumber", "HDP-2026-0001").param("dateOfBirth", "15-04-1990")
                        .with(as(AARAV)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'dateOfBirth' has an invalid value '15-04-1990'"));
    }
}
