// ============================================================================
// OPTIONAL: FR-5 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: SRCH-V-01/02 unit tests for SearchCriteria.
// Only FR-5 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-5" (search the project for it). Theory and flow: docs/steps/step8.readme.md
// ============================================================================
package com.healthdekho.service;

import com.healthdekho.exception.BusinessRuleException;
import com.healthdekho.service.SearchCriteria.ByCustomerName;
import com.healthdekho.service.SearchCriteria.ByPolicyNumber;
import com.healthdekho.service.SearchCriteria.ByPolicyNumberAndDateOfBirth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SRCH-V - search criteria validation")
class SearchCriteriaTest {

    private static final LocalDate DOB = LocalDate.of(1990, 4, 15);

    @Test
    @DisplayName("SRCH-V-01 the three allowed combinations")
    void allowedCombinations() {
        assertThat(SearchCriteria.of(" hdp-2026-0001 ", null, null))
                .isEqualTo(new ByPolicyNumber("HDP-2026-0001"));
        assertThat(SearchCriteria.of(null, "Aarav", null))
                .isEqualTo(new ByCustomerName("Aarav"));
        assertThat(SearchCriteria.of("HDP-2026-0001", "  ", DOB))
                .isEqualTo(new ByPolicyNumberAndDateOfBirth("HDP-2026-0001", DOB));
    }

    @Test
    @DisplayName("SRCH-V-02 rejected combinations and formats")
    void rejected() {
        assertThatThrownBy(() -> SearchCriteria.of(null, null, null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> SearchCriteria.of("HDP-2026-0001", "Aarav", null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> SearchCriteria.of(null, "Aarav", DOB)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> SearchCriteria.of("HDP 2026 0001", null, null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> SearchCriteria.of(null, "a", null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> SearchCriteria.of(null, "Aar_av%", null)).isInstanceOf(BusinessRuleException.class);
    }
}
