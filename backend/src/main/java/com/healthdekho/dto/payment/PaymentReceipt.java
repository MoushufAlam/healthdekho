// ============================================================================
// OPTIONAL: FR-4 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: receipt for one paid installment (transaction ref, penalty, new status).
// Only FR-4 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-4" (search the project for it). Theory and flow: docs/steps/step7.readme.md
// ============================================================================
package com.healthdekho.dto.payment;

import com.healthdekho.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentReceipt(
        String transactionReference,
        Long installmentId,
        int installmentNumber,
        LocalDate paymentDate,
        BigDecimal amountPaid,
        BigDecimal penaltyAmount,
        PaymentStatus installmentStatus) {
}
