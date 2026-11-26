// ============================================================================
// OPTIONAL: FR-4 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: RULE-01..05 unit tests for PremiumRules.
// Only FR-4 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-4" (search the project for it). Theory and flow: docs/steps/step7.readme.md
// ============================================================================
package com.healthdekho.service;

import com.healthdekho.entity.PremiumInstallment;
import com.healthdekho.entity.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RULE - installment money and status rules")
class PremiumRulesTest {

    private static final LocalDate GRACE_END = LocalDate.of(2027, 1, 20);

    private static PremiumInstallment installment(String scheduled, String paid, PaymentStatus status) {
        PremiumInstallment installment = new PremiumInstallment();
        installment.setScheduledAmount(new BigDecimal(scheduled));
        installment.setPaidAmount(new BigDecimal(paid));
        installment.setPaymentStatus(status);
        installment.setDueDate(GRACE_END.minusMonths(1));
        installment.setGracePeriodEndDate(GRACE_END);
        return installment;
    }

    @Test
    @DisplayName("RULE-01 no penalty on the last day of grace, 10% from the next day")
    void penalty_startsAfterGraceEnd() {
        PremiumInstallment pending = installment("1250.00", "0.00", PaymentStatus.PENDING);

        assertThat(PremiumRules.penaltyIfPaidOn(pending, GRACE_END)).isEqualByComparingTo("0");
        assertThat(PremiumRules.penaltyIfPaidOn(pending, GRACE_END.plusDays(1))).isEqualByComparingTo("125.00");
        assertThat(PremiumRules.amountDueOn(pending, GRACE_END.plusDays(1))).isEqualByComparingTo("1375.00");
    }

    @Test
    @DisplayName("RULE-02 penalty is 10% of the OUTSTANDING premium, rounded half-up to paise")
    void penalty_onOutstandingOnly() {
        PremiumInstallment partial = installment("1250.00", "500.05", PaymentStatus.PARTIALLY_PAID);

        // outstanding 749.95 -> 74.995 -> 75.00
        assertThat(PremiumRules.penaltyIfPaidOn(partial, GRACE_END.plusDays(1))).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("RULE-03 paid installment: nothing outstanding, extra above schedule counts as penalty")
    void paidInstallment_totals() {
        PremiumInstallment paidLate = installment("1250.00", "1375.00", PaymentStatus.PAID);

        assertThat(PremiumRules.outstandingPremium(paidLate)).isEqualByComparingTo("0");
        assertThat(PremiumRules.penaltyIfPaidOn(paidLate, GRACE_END.plusDays(10))).isEqualByComparingTo("0");
        assertThat(PremiumRules.premiumPaid(paidLate)).isEqualByComparingTo("1250.00");
        assertThat(PremiumRules.penaltyPaid(paidLate)).isEqualByComparingTo("125.00");
    }

    @ParameterizedTest(name = "{0} paid {1}, checked {2} days after grace end -> {3}")
    @CsvSource({
            "PENDING,        0.00,    -1, PENDING",
            "PENDING,        0.00,     1, OVERDUE",
            "PARTIALLY_PAID, 500.00,  -5, PARTIALLY_PAID",
            "PARTIALLY_PAID, 500.00,   5, OVERDUE",
            "PAID,           1250.00,  5, PAID",
            "OVERDUE,        0.00,    -1, PENDING"
    })
    @DisplayName("RULE-04 effective status is derived from the date")
    void effectiveStatus(PaymentStatus stored, String paid, int daysAfterGrace, PaymentStatus expected) {
        PremiumInstallment installment = installment("1250.00", paid, stored);

        assertThat(PremiumRules.effectiveStatus(installment, GRACE_END.plusDays(daysAfterGrace))).isEqualTo(expected);
    }

    @ParameterizedTest(name = "premium paid {0} of 1250 -> {1}")
    @CsvSource({"0.00, PENDING", "0.01, PARTIALLY_PAID", "1249.99, PARTIALLY_PAID", "1250.00, PAID"})
    @DisplayName("RULE-05 status after a payment")
    void statusAfterPayment(String premiumPaid, PaymentStatus expected) {
        assertThat(PremiumRules.statusAfterPayment(new BigDecimal(premiumPaid), new BigDecimal("1250.00")))
                .isEqualTo(expected);
    }
}
