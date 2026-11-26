package com.healthdekho.dto.policy;

import com.healthdekho.dto.customer.CustomerProfileResponse;
import com.healthdekho.dto.product.ProductResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

// Everything the FR-3 registration form shows before the customer clicks "Register"
public record RegistrationPreviewResponse(
        CustomerProfileResponse customer,
        ProductResponse product,
        LocalDate startDate,
        LocalDate maturityDate,
        int numberOfInstallments,
        BigDecimal totalPremiumPayable,
        LocalDate firstDueDate,
        LocalDate lastDueDate,
        boolean eligible,
        String eligibilityMessage) {
}
