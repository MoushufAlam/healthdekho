// ============================================================================
// OPTIONAL: FR-4 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: FR-4B one row of the installment schedule (status, penalty if paid today).
// Only FR-4 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-4" (search the project for it). Theory and flow: docs/steps/step7.readme.md
// ============================================================================
package com.healthdekho.dto.policy;

import com.healthdekho.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

// One row of the FR-4B schedule. amountDueToday already includes the 10% penalty when past grace.
public record InstallmentResponse(
        Long installmentId,
        int installmentNumber,
        YearMonth period,
        BigDecimal scheduledAmount,
        LocalDate dueDate,
        LocalDate gracePeriodEndDate,
        BigDecimal paidAmount,
        BigDecimal outstandingPremium,
        BigDecimal penaltyIfPaidToday,
        BigDecimal amountDueToday,
        PaymentStatus paymentStatus,
        boolean selectable) {
}
