// ============================================================================
// OPTIONAL: FR-4 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: FR-4A policy lookup: header + total payable / paid / remaining.
// Only FR-4 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-4" (search the project for it). Theory and flow: docs/steps/step7.readme.md
// ============================================================================
package com.healthdekho.dto.policy;

import com.healthdekho.entity.enums.PaymentStatus;
import com.healthdekho.entity.enums.PolicyStatus;
import com.healthdekho.entity.enums.PolicyType;
import com.healthdekho.entity.enums.PremiumFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

// FR-4A policy lookup header. Premium totals exclude late penalties; penalties are reported separately.
public record PolicyDetailResponse(
        String policyNumber,
        String customerName,
        String policyName,
        PolicyType policyType,
        int termMonths,
        LocalDate startDate,
        LocalDate maturityDate,
        PremiumFrequency premiumFrequency,
        BigDecimal premiumAmount,
        BigDecimal totalPremiumPayable,
        BigDecimal totalPremiumPaid,
        BigDecimal totalRemainingPremium,
        BigDecimal totalPenaltyPaid,
        PolicyStatus status,
        Map<PaymentStatus, Long> installmentStatusCounts) {
}
