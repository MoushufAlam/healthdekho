package com.healthdekho.dto.product;

import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.entity.enums.PolicyType;
import com.healthdekho.entity.enums.PremiumFrequency;

import java.math.BigDecimal;

public record ProductResponse(
        Long productId,
        String productCode,
        String policyName,
        PolicyType policyType,
        int termMonths,
        BigDecimal sumInsured,
        PremiumFrequency premiumFrequency,
        BigDecimal premiumAmount,
        int minEntryAge,
        int maxEntryAge) {

    public static ProductResponse from(InsuranceProduct product) {
        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getPolicyName(),
                product.getPolicyType(),
                product.getTermMonths(),
                product.getSumInsured(),
                product.getPremiumFrequency(),
                product.getPremiumAmount(),
                product.getMinEntryAge(),
                product.getMaxEntryAge());
    }
}
