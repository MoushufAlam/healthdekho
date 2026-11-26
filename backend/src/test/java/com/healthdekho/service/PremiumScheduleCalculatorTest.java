package com.healthdekho.service;

import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.entity.enums.PremiumFrequency;
import com.healthdekho.exception.BusinessRuleException;
import com.healthdekho.service.PremiumScheduleCalculator.PremiumPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CALC - maturity date, total premium and installment schedule")
class PremiumScheduleCalculatorTest {

    private final PremiumScheduleCalculator calculator = new PremiumScheduleCalculator();

    private static InsuranceProduct product(int termMonths, PremiumFrequency frequency, String premium) {
        InsuranceProduct product = new InsuranceProduct();
        product.setProductCode("TEST");
        product.setTermMonths(termMonths);
        product.setPremiumFrequency(frequency);
        product.setPremiumAmount(new BigDecimal(premium));
        return product;
    }

    @Test
    @DisplayName("CALC-01 README sample: 240-month monthly term at 1250 = 300000, matures 2046-11-19")
    void monthlyTerm_matchesReadmeSample() {
        PremiumPlan plan = calculator.plan(product(240, PremiumFrequency.MONTHLY, "1250.00"), LocalDate.of(2026, 11, 20));

        assertThat(plan.numberOfInstallments()).isEqualTo(240);
        assertThat(plan.totalPremiumPayable()).isEqualByComparingTo("300000.00");
        assertThat(plan.maturityDate()).isEqualTo(LocalDate.of(2046, 11, 19));
        assertThat(plan.installments().get(1).dueDate()).isEqualTo(LocalDate.of(2026, 12, 20));
        assertThat(plan.installments().get(1).gracePeriodEndDate()).isEqualTo(LocalDate.of(2027, 1, 20));
        assertThat(plan.lastDueDate()).isEqualTo(LocalDate.of(2046, 10, 20));
    }

    @Test
    @DisplayName("CALC-02 quarterly: 120 months = 40 installments, 3 months apart")
    void quarterly() {
        PremiumPlan plan = calculator.plan(product(120, PremiumFrequency.QUARTERLY, "7500.00"), LocalDate.of(2026, 11, 26));

        assertThat(plan.numberOfInstallments()).isEqualTo(40);
        assertThat(plan.totalPremiumPayable()).isEqualByComparingTo("300000.00");
        assertThat(plan.installments().get(1).dueDate()).isEqualTo(LocalDate.of(2027, 2, 26));
    }

    @Test
    @DisplayName("CALC-03 yearly single-year product has exactly one installment")
    void yearly() {
        PremiumPlan plan = calculator.plan(product(12, PremiumFrequency.YEARLY, "18000.00"), LocalDate.of(2026, 11, 20));

        assertThat(plan.numberOfInstallments()).isEqualTo(1);
        assertThat(plan.maturityDate()).isEqualTo(LocalDate.of(2027, 11, 19));
        assertThat(plan.firstDueDate()).isEqualTo(plan.lastDueDate());
    }

    @Test
    @DisplayName("CALC-04 month-end start dates don't drift (31 Jan -> 28 Feb -> 31 Mar)")
    void monthEnd_noDrift() {
        PremiumPlan plan = calculator.plan(product(12, PremiumFrequency.MONTHLY, "100.00"), LocalDate.of(2027, 1, 31));

        assertThat(plan.installments().get(1).dueDate()).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(plan.installments().get(2).dueDate()).isEqualTo(LocalDate.of(2027, 3, 31));
    }

    @Test
    @DisplayName("CALC-05 a term that does not split into whole installments is rejected")
    void termNotDivisible() {
        assertThatThrownBy(() -> calculator.plan(product(10, PremiumFrequency.QUARTERLY, "100.00"), LocalDate.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("does not fit QUARTERLY");
    }
}
