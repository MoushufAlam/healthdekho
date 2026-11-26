package com.healthdekho.dto.recommendation;

import com.healthdekho.entity.enums.PolicyType;
import com.healthdekho.entity.enums.PremiumFrequency;
import com.healthdekho.entity.enums.Suitability;

import java.math.BigDecimal;

// recommendationItemId is sent back on registration so we can mark the item as selected
public record RecommendedProductResponse(
        Long recommendationItemId,
        int rank,
        Long productId,
        String productCode,
        String policyName,
        PolicyType policyType,
        int termMonths,
        BigDecimal sumInsured,
        PremiumFrequency premiumFrequency,
        BigDecimal premiumAmount,
        String recommendationReason,
        Suitability suitability) {
}
