package com.healthdekho.service;

import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class PremiumScheduleCalculator {

    static final int GRACE_PERIOD_MONTHS = 1;

    public PremiumPlan plan(InsuranceProduct product, LocalDate startDate) {
        int monthsPerInstallment = product.getPremiumFrequency().getMonths();
        int termMonths = product.getTermMonths();
        if (termMonths % monthsPerInstallment != 0) {
            throw new BusinessRuleException("Product %s has a term of %d months that does not fit %s payments"
                    .formatted(product.getProductCode(), termMonths, product.getPremiumFrequency()));
        }
        int numberOfInstallments = termMonths / monthsPerInstallment;

        // last day of cover: 20 Nov 2026 + 12 months - 1 day = 19 Nov 2027
        LocalDate maturityDate = startDate.plusMonths(termMonths).minusDays(1);

        BigDecimal premium = product.getPremiumAmount();
        BigDecimal totalPremiumPayable = premium.multiply(BigDecimal.valueOf(numberOfInstallments))
                .setScale(2, RoundingMode.HALF_UP);

        // always offset from the start date (not from the previous due date) so a 31st doesn't drift to the 28th
        List<ScheduledInstallment> installments = IntStream.rangeClosed(1, numberOfInstallments)
                .mapToObj(number -> {
                    LocalDate dueDate = startDate.plusMonths((long) (number - 1) * monthsPerInstallment);
                    return new ScheduledInstallment(number, dueDate, dueDate.plusMonths(GRACE_PERIOD_MONTHS), premium);
                })
                .toList();

        return new PremiumPlan(startDate, maturityDate, numberOfInstallments, totalPremiumPayable, installments);
    }

    public record PremiumPlan(
            LocalDate startDate,
            LocalDate maturityDate,
            int numberOfInstallments,
            BigDecimal totalPremiumPayable,
            List<ScheduledInstallment> installments) {

        public LocalDate firstDueDate() {
            return installments.getFirst().dueDate();
        }

        public LocalDate lastDueDate() {
            return installments.getLast().dueDate();
        }
    }

    public record ScheduledInstallment(int number, LocalDate dueDate, LocalDate gracePeriodEndDate, BigDecimal amount) {
    }
}
