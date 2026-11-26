package com.healthdekho.service;

import com.healthdekho.dto.product.ProductResponse;
import com.healthdekho.entity.enums.PolicyType;
import com.healthdekho.entity.enums.PremiumFrequency;
import com.healthdekho.entity.enums.Suitability;
import com.healthdekho.service.RuleBasedRecommender.Ranked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FALLBACK - rule-based recommender and age helper")
class RuleBasedRecommenderTest {

    private final RuleBasedRecommender recommender = new RuleBasedRecommender();

    private static ProductResponse product(String code, PolicyType type, int termMonths, String premium) {
        return new ProductResponse(1L, code, code, type, termMonths, new BigDecimal("100000"),
                PremiumFrequency.YEARLY, new BigDecimal(premium), 18, 65);
    }

    private final List<ProductResponse> catalog = List.of(
            product("TERM", PolicyType.TERM, 240, "1250"),
            product("HEALTH", PolicyType.HEALTH, 12, "18000"),
            product("SAV", PolicyType.SAVINGS, 120, "7500"));

    @Test
    @DisplayName("FALLBACK-01 preferred type + matching goal + close term ranks first with HIGH suitability")
    void bestMatchFirst() {
        List<Ranked> ranked = recommender.rank(catalog, "Health cover for my parents", "1 year", PolicyType.HEALTH, 3);

        assertThat(ranked).extracting(r -> r.product().productCode()).containsExactly("HEALTH", "TERM", "SAV");
        assertThat(ranked.getFirst().suitability()).isEqualTo(Suitability.HIGH);
        assertThat(ranked.getFirst().reason()).startsWith("Rule-based match:");
        assertThat(ranked.get(1).suitability()).isEqualTo(Suitability.LOW);
    }

    @Test
    @DisplayName("FALLBACK-02 no preferences: cheapest premium first, limited to N")
    void noPreferences_cheapestFirst() {
        List<Ranked> ranked = recommender.rank(catalog, null, null, null, 2);

        assertThat(ranked).extracting(r -> r.product().productCode()).containsExactly("TERM", "SAV");
    }

    @Test
    @DisplayName("FALLBACK-03 preferred term text is understood in years or months")
    void parseTerm() {
        assertThat(RuleBasedRecommender.parseTermMonths("20 years")).isEqualTo(OptionalInt.of(240));
        assertThat(RuleBasedRecommender.parseTermMonths("about 18 months")).isEqualTo(OptionalInt.of(18));
        assertThat(RuleBasedRecommender.parseTermMonths("long")).isEmpty();
        assertThat(RuleBasedRecommender.parseTermMonths(null)).isEmpty();
    }

    @Test
    @DisplayName("AGE-01 age counts completed years only")
    void ageOnBirthday() {
        LocalDate dob = LocalDate.of(1990, 4, 15);

        assertThat(AgeCalculator.ageOn(dob, LocalDate.of(2026, 4, 14))).isEqualTo(35);
        assertThat(AgeCalculator.ageOn(dob, LocalDate.of(2026, 4, 15))).isEqualTo(36);
    }
}
