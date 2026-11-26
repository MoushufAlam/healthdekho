// ============================================================================
// OPTIONAL: FR-5 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: one search result row (FR-5 columns).
// Only FR-5 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-5" (search the project for it). Theory and flow: docs/steps/step8.readme.md
// ============================================================================
package com.healthdekho.dto.search;

import com.healthdekho.entity.enums.PolicyType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicySearchResultResponse(
        String policyNumber,
        String customerName,
        LocalDate dateOfBirth,
        String policyName,
        PolicyType policyType,
        LocalDate startDate,
        LocalDate maturityDate,
        BigDecimal totalPremiumPayable,
        PremiumPaymentStatus premiumPaymentStatus,
        BigDecimal totalPremiumPaid,
        BigDecimal remainingPremium) {
}
