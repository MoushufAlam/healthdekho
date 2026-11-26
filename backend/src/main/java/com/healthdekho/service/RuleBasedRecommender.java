package com.healthdekho.service;

import com.healthdekho.dto.product.ProductResponse;
import com.healthdekho.entity.enums.PolicyType;
import com.healthdekho.entity.enums.Suitability;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Deterministic ranking used whenever the AI is unavailable or returns nothing usable.
// Same input always gives the same output, so the fallback is easy to explain and test.
@Component
public class RuleBasedRecommender {

    public static final String MODEL_NAME = "rule-based-fallback";

    private static final Pattern TERM = Pattern.compile("(\\d{1,3})\\s*(year|yr|month|mo)", Pattern.CASE_INSENSITIVE);

    private static final Map<PolicyType, List<String>> GOAL_KEYWORDS = Map.of(
            PolicyType.HEALTH, List.of("health", "medical", "hospital", "parents", "illness"),
            PolicyType.TERM, List.of("family", "income", "protection", "dependents", "loan"),
            PolicyType.SAVINGS, List.of("saving", "wealth", "retire", "child", "education", "invest"),
            PolicyType.LIFE, List.of("life", "legacy", "whole life"));

    public List<Ranked> rank(List<ProductResponse> candidates, String goals, String preferredTerm,
                             PolicyType preferredType, int limit) {
        OptionalInt preferredMonths = parseTermMonths(preferredTerm);
        String goalText = goals == null ? "" : goals.toLowerCase(Locale.ROOT);

        return candidates.stream()
                .map(product -> score(product, goalText, preferredMonths, preferredType))
                .sorted(Comparator.comparingInt(Ranked::score).reversed()
                        .thenComparing(r -> r.product().premiumAmount()))
                .limit(limit)
                .toList();
    }

    static OptionalInt parseTermMonths(String preferredTerm) {
        if (preferredTerm == null) {
            return OptionalInt.empty();
        }
        Matcher matcher = TERM.matcher(preferredTerm);
        if (!matcher.find()) {
            return OptionalInt.empty();
        }
        int value = Integer.parseInt(matcher.group(1));
        return matcher.group(2).toLowerCase(Locale.ROOT).startsWith("y") ? OptionalInt.of(value * 12)
                : OptionalInt.of(value);
    }

    private Ranked score(ProductResponse product, String goalText, OptionalInt preferredMonths,
                         PolicyType preferredType) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (product.policyType() == preferredType) {
            score += 3;
            reasons.add("matches your preferred %s cover".formatted(product.policyType()));
        }
        boolean goalMatch = GOAL_KEYWORDS.getOrDefault(product.policyType(), List.of()).stream()
                .anyMatch(goalText::contains);
        if (goalMatch) {
            score += 2;
            reasons.add("fits your stated goals");
        }
        if (preferredMonths.isPresent()) {
            int gap = Math.abs(product.termMonths() - preferredMonths.getAsInt());
            if (gap <= 12) {
                score += 2;
                reasons.add("term of %d months is close to your preferred term".formatted(product.termMonths()));
            } else if (gap <= 60) {
                score += 1;
            }
        }
        if (reasons.isEmpty()) {
            reasons.add("eligible for your age and not yet in your portfolio");
        }

        Suitability suitability = score >= 4 ? Suitability.HIGH : score >= 2 ? Suitability.MEDIUM : Suitability.LOW;
        String reason = "Rule-based match: " + String.join("; ", reasons) + ".";
        return new Ranked(product, score, reason, suitability);
    }

    public record Ranked(ProductResponse product, int score, String reason, Suitability suitability) {
    }
}
