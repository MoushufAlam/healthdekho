// ============================================================================
// OPTIONAL: FR-4 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: PAY-01..13 tests, incl. PAY-09 data isolation (another customer's policy -> 404).
// Only FR-4 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-4" (search the project for it). Theory and flow: docs/steps/step7.readme.md
// ============================================================================
package com.healthdekho.controller;

import com.healthdekho.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PAY - policy lookup, schedule and premium payments (FR-4)")
class PaymentIntegrationTest extends IntegrationTestBase {

    private static final String AARAV_POLICY = "/api/policies/HDP-2026-0001";

    private String payment(String method, Object... installmentAndAmount) throws Exception {
        List<Map<String, Object>> lines = new java.util.ArrayList<>();
        for (int i = 0; i < installmentAndAmount.length; i += 2) {
            lines.add(Map.of("installmentId", installmentAndAmount[i], "amount", installmentAndAmount[i + 1]));
        }
        return json(Map.of("paymentMethod", method, "installments", lines));
    }

    @Test
    @DisplayName("PAY-01 policy lookup shows totals payable, paid and remaining")
    void detail_showsTotals() throws Exception {
        mockMvc.perform(get(AARAV_POLICY).with(as(AARAV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Aarav Sharma"))
                .andExpect(jsonPath("$.totalPremiumPayable").value(300000.00))
                .andExpect(jsonPath("$.totalPremiumPaid").value(1750.00))
                .andExpect(jsonPath("$.totalRemainingPremium").value(298250.00))
                .andExpect(jsonPath("$.installmentStatusCounts.PAID").value(1))
                .andExpect(jsonPath("$.installmentStatusCounts.PARTIALLY_PAID").value(1));
    }

    @Test
    @DisplayName("PAY-02 schedule lists every installment with status and what is due today")
    void installments_listed() throws Exception {
        mockMvc.perform(get(AARAV_POLICY + "/installments").with(as(AARAV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(240)))
                .andExpect(jsonPath("$[0].paymentStatus").value("PAID"))
                .andExpect(jsonPath("$[0].selectable").value(false))
                .andExpect(jsonPath("$[1].paymentStatus").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$[1].amountDueToday").value(750.00))
                .andExpect(jsonPath("$[1].period").value("2026-12"));
    }

    @Test
    @DisplayName("PAY-03 partial payment within grace updates status and remaining amount")
    void partialPayment_withinGrace() throws Exception {
        mockMvc.perform(post(AARAV_POLICY + "/payments").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("UPI", 4003, 500)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receipts[0].installmentStatus").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.receipts[0].penaltyAmount").value(0.00))
                .andExpect(jsonPath("$.receipts[0].transactionReference", containsString("TXN-HD-")))
                .andExpect(jsonPath("$.totalPremiumPaid").value(2250.00))
                .andExpect(jsonPath("$.totalRemainingPremium").value(297750.00));

        mockMvc.perform(get(AARAV_POLICY + "/payments").with(as(AARAV)))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].installmentNumber", hasItem(3)));
    }

    @Test
    @DisplayName("PAY-04 several installments in one request; completing one marks it PAID")
    void multiplePayments_completeInstallment() throws Exception {
        mockMvc.perform(post(AARAV_POLICY + "/payments").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("CARD", 4002, 750, 4003, 1250)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receipts", hasSize(2)))
                .andExpect(jsonPath("$.receipts[0].installmentStatus").value("PAID"))
                .andExpect(jsonPath("$.receipts[1].installmentStatus").value("PAID"))
                .andExpect(jsonPath("$.totalPaidNow").value(2000.00));
    }

    @Test
    @DisplayName("PAY-05 paying more than the outstanding amount is rejected")
    void overpayment_400() throws Exception {
        mockMvc.perform(post(AARAV_POLICY + "/payments").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("UPI", 4002, 800)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("exceeds the outstanding premium 750.00")));
    }

    @Test
    @DisplayName("PAY-06 paying an already paid installment is rejected (duplicate payment)")
    void alreadyPaid_409() throws Exception {
        mockMvc.perform(post(AARAV_POLICY + "/payments").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("UPI", 4001, 100)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Installment #1 is already paid"));
    }

    @Test
    @DisplayName("PAY-07 after the grace period a 10% penalty applies and the full amount is required")
    void latePayment_penalty() throws Exception {
        jdbc.update("""
                update premium_installment set due_date = '2026-09-20', grace_period_end_date = '2026-10-20'
                where installment_id = 4003""");

        mockMvc.perform(post(AARAV_POLICY + "/payments").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("UPI", 4003, 1250)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Pay the full amount due 1375.00")));

        mockMvc.perform(post(AARAV_POLICY + "/payments").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("NET_BANKING", 4003, "1375.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receipts[0].penaltyAmount").value(125.00))
                .andExpect(jsonPath("$.receipts[0].installmentStatus").value("PAID"))
                .andExpect(jsonPath("$.totalPremiumPaid").value(3000.00));

        mockMvc.perform(get(AARAV_POLICY).with(as(AARAV)))
                .andExpect(jsonPath("$.totalPenaltyPaid").value(125.00));
    }

    @Test
    @DisplayName("PAY-08 overdue installment shows OVERDUE and the penalty in the schedule")
    void overdueShownInSchedule() throws Exception {
        jdbc.update("""
                update premium_installment set due_date = '2026-09-20', grace_period_end_date = '2026-10-20'
                where installment_id = 4003""");

        mockMvc.perform(get(AARAV_POLICY + "/installments").with(as(AARAV)))
                .andExpect(jsonPath("$[2].paymentStatus").value("OVERDUE"))
                .andExpect(jsonPath("$[2].penaltyIfPaidToday").value(125.00))
                .andExpect(jsonPath("$[2].amountDueToday").value(1375.00));
    }

    @Test
    @DisplayName("PAY-09 ISOLATION: customer B cannot view or pay customer A's policy (404)")
    void otherCustomersPolicy_404() throws Exception {
        mockMvc.perform(get(AARAV_POLICY).with(as(MEERA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Policy HDP-2026-0001 not found"));

        mockMvc.perform(get(AARAV_POLICY + "/installments").with(as(MEERA)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(AARAV_POLICY + "/payments").with(as(MEERA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("UPI", 4003, 100)))
                .andExpect(status().isNotFound());

        Long payments = jdbc.queryForObject("select count(*) from premium_payment where installment_id = 4003",
                Long.class);
        assertThat(payments).isZero();
    }

    @Test
    @DisplayName("PAY-10 installment from a different policy is not found")
    void installmentOfOtherPolicy_404() throws Exception {
        mockMvc.perform(post(AARAV_POLICY + "/payments").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("UPI", 4004, 100)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("[4004] not found")));
    }

    @Test
    @DisplayName("PAY-11 one failing line rolls back the whole request")
    // no surrounding test transaction here, so the service's own transaction really commits or rolls back
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void oneBadLine_rollsBackAll() throws Exception {
        mockMvc.perform(post(AARAV_POLICY + "/payments").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("UPI", 4003, 500, 4005, 99999)))
                .andExpect(status().isBadRequest());

        Long payments = jdbc.queryForObject("select count(*) from premium_payment where installment_id = 4003",
                Long.class);
        assertThat(payments).isZero();
    }

    @Test
    @DisplayName("PAY-12 invalid input: zero amount, 3 decimals, duplicate installment, missing method")
    void invalidInput_400() throws Exception {
        mockMvc.perform(post(AARAV_POLICY + "/payments").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("UPI", 4003, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['installments[0].amount']").exists());

        mockMvc.perform(post(AARAV_POLICY + "/payments").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("UPI", 4003, "10.555")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(AARAV_POLICY + "/payments").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("UPI", 4003, 100, 4003, 100)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The same installment was selected more than once"));

        mockMvc.perform(post(AARAV_POLICY + "/payments").with(as(AARAV))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"installments\":[{\"installmentId\":4003,\"amount\":100}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.paymentMethod").exists());
    }

    @Test
    @DisplayName("PAY-13 paying the last installment marks the policy FULLY_PAID")
    void lastInstallment_policyFullyPaid() throws Exception {
        String body = mockMvc.perform(post("/api/policies").with(as(ROHAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", 202))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String policyNumber = objectMapper.readTree(body).get("policyNumber").asText();
        Long installmentId = jdbc.queryForObject("""
                select i.installment_id from premium_installment i join customer_policy p on p.policy_id = i.policy_id
                where p.policy_number = ?""", Long.class, policyNumber);

        mockMvc.perform(post("/api/policies/" + policyNumber + "/payments").with(as(ROHAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("CARD", installmentId, 18000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policyStatus").value("FULLY_PAID"))
                .andExpect(jsonPath("$.totalRemainingPremium").value(0.00));

        mockMvc.perform(post("/api/policies/" + policyNumber + "/payments").with(as(ROHAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment("CARD", installmentId, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already fully paid")));
    }
}
