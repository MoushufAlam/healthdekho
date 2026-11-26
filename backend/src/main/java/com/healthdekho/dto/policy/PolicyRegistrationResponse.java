package com.healthdekho.dto.policy;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyRegistrationResponse(
        String policyNumber,
        String message,
        PolicySummaryResponse policy,
        int numberOfInstallments,
        BigDecimal totalPremiumPayable,
        LocalDate firstDueDate) {
}
