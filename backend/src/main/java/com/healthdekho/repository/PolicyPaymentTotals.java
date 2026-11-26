// ============================================================================
// OPTIONAL: FR-5 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: per-policy totals returned by PremiumInstallmentRepository.sumByPolicyIds.
// Only FR-5 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-5" (search the project for it). Theory and flow: docs/steps/step8.readme.md
// ============================================================================
package com.healthdekho.repository;

import java.math.BigDecimal;

// Aggregated per policy in one SQL query, so search results don't load every installment row
public record PolicyPaymentTotals(
        Long policyId,
        BigDecimal premiumPaid,
        BigDecimal penaltyPaid,
        Long overdueInstallments) {
}
