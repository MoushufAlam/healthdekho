package com.healthdekho.dto.policy;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record PolicyRegistrationRequest(
        @NotNull(message = "Product is required")
        @Positive(message = "Product id must be positive")
        Long productId,

        // optional: defaults to today; checked against the app clock in the service, not here
        LocalDate startDate,

        // optional: set when the customer registers from an AI recommendation
        @Positive(message = "Recommendation item id must be positive")
        Long recommendationItemId) {
}
