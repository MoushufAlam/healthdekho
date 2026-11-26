package com.healthdekho.dto.policy;

import com.healthdekho.entity.CustomerPolicy;
import com.healthdekho.entity.enums.PolicyStatus;
import com.healthdekho.entity.enums.PolicyType;
import com.healthdekho.entity.enums.PremiumFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;

// One row of the dashboard table (FR-1B)
public record PolicySummaryResponse(
        String policyNumber,
        String policyName,
        PolicyType policyType,
        int termMonths,
        LocalDate startDate,
        LocalDate maturityDate,
        PremiumFrequency premiumFrequency,
        BigDecimal premiumAmount,
        BigDecimal sumInsured,
        PolicyStatus status) {

    public static PolicySummaryResponse from(CustomerPolicy policy) {
        return new PolicySummaryResponse(
                policy.getPolicyNumber(),
                policy.getProduct().getPolicyName(),
                policy.getProduct().getPolicyType(),
                policy.getProduct().getTermMonths(),
                policy.getStartDate(),
                policy.getMaturityDate(),
                policy.getPremiumFrequency(),
                policy.getPremiumAmount(),
                policy.getSumInsured(),
                policy.getStatus());
    }
}
