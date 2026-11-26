// ============================================================================
// OPTIONAL: FR-4 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: response of a payment: receipts + updated policy totals.
// Only FR-4 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-4" (search the project for it). Theory and flow: docs/steps/step7.readme.md
// ============================================================================
package com.healthdekho.dto.payment;

import com.healthdekho.entity.enums.PolicyStatus;

import java.math.BigDecimal;
import java.util.List;

public record PaymentResponse(
        String policyNumber,
        List<PaymentReceipt> receipts,
        BigDecimal totalPaidNow,
        BigDecimal totalPremiumPaid,
        BigDecimal totalRemainingPremium,
        PolicyStatus policyStatus,
        String message) {
}
