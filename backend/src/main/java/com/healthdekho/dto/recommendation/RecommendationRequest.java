package com.healthdekho.dto.recommendation;

import com.healthdekho.entity.enums.PolicyType;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// All fields optional, but at least one must be given (checked in the service).
// dateOfBirth falls back to the customer's stored DOB.
public record RecommendationRequest(
        LocalDate dateOfBirth,

        @Size(max = 1000, message = "Insurance goals can be at most 1000 characters")
        String insuranceGoals,

        @Size(max = 50, message = "Preferred term can be at most 50 characters")
        String preferredTerm,

        PolicyType preferredPolicyType) {

    public boolean hasAnyInput() {
        return dateOfBirth != null
                || (insuranceGoals != null && !insuranceGoals.isBlank())
                || (preferredTerm != null && !preferredTerm.isBlank())
                || preferredPolicyType != null;
    }
}
