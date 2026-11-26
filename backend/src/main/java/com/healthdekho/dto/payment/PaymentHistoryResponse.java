// ============================================================================
// OPTIONAL: FR-4 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: one row of GET /api/policies/{policyNumber}/payments.
// Only FR-4 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-4" (search the project for it). Theory and flow: docs/steps/step7.readme.md
// ============================================================================
package com.healthdekho.dto.payment;

import com.healthdekho.entity.PremiumPayment;
import com.healthdekho.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentHistoryResponse(
        String transactionReference,
        int installmentNumber,
        LocalDate paymentDate,
        BigDecimal amountPaid,
        BigDecimal penaltyAmount,
        PaymentMethod paymentMethod) {

    public static PaymentHistoryResponse from(PremiumPayment payment) {
        return new PaymentHistoryResponse(
                payment.getTransactionReference(),
                payment.getInstallment().getInstallmentNumber(),
                payment.getPaymentDate(),
                payment.getAmountPaid(),
                payment.getPenaltyAmount(),
                payment.getPaymentMethod());
    }
}
