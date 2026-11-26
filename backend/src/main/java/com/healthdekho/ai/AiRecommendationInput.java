package com.healthdekho.ai;

import com.healthdekho.dto.product.ProductResponse;
import com.healthdekho.entity.enums.PolicyType;

import java.util.List;

// candidates are already filtered by Java (active, not owned, age-eligible); the AI only ranks and explains
public record AiRecommendationInput(
        int age,
        String insuranceGoals,
        String preferredTerm,
        PolicyType preferredPolicyType,
        List<ProductResponse> candidates,
        int maxRecommendations) {
}
