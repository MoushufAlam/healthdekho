package com.healthdekho.ai;

import com.healthdekho.dto.product.ProductResponse;

import java.util.stream.Collectors;

// Bump app.ai.prompt-version whenever this text changes; it is stored with every request for audit.
final class RecommendationPrompt {

    static final String SYSTEM = """
            You are an insurance advisor for HealthDekho.
            Recommend products ONLY from the CANDIDATE PRODUCTS list, using their exact productCode.
            Never invent products, prices or benefits that are not in the list.
            Rank the best fit first (rank 1). For each product give:
            - reason: one or two plain sentences tied to the customer's goals, age and preferences
            - suitability: HIGH, MEDIUM or LOW
            Return at most %d products.
            """;

    private RecommendationPrompt() {
    }

    static String system(AiRecommendationInput input) {
        return SYSTEM.formatted(input.maxRecommendations());
    }

    static String user(AiRecommendationInput input) {
        String candidates = input.candidates().stream()
                .map(RecommendationPrompt::describe)
                .collect(Collectors.joining("\n"));
        return """
                CUSTOMER
                - age: %d
                - insurance goals: %s
                - preferred term: %s
                - preferred policy type: %s

                CANDIDATE PRODUCTS
                %s
                """.formatted(
                input.age(),
                orNotGiven(input.insuranceGoals()),
                orNotGiven(input.preferredTerm()),
                input.preferredPolicyType() == null ? "not given" : input.preferredPolicyType(),
                candidates);
    }

    private static String describe(ProductResponse p) {
        return "- productCode=%s | name=%s | type=%s | termMonths=%d | frequency=%s | premium=%s | sumInsured=%s"
                .formatted(p.productCode(), p.policyName(), p.policyType(), p.termMonths(),
                        p.premiumFrequency(), p.premiumAmount(), p.sumInsured());
    }

    private static String orNotGiven(String value) {
        return value == null || value.isBlank() ? "not given" : value.trim();
    }
}
