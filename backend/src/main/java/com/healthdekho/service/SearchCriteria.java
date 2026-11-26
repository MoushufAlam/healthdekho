// ============================================================================
// OPTIONAL: FR-5 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: sealed interface that validates the search parameter combination.
// Only FR-5 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-5" (search the project for it). Theory and flow: docs/steps/step8.readme.md
// ============================================================================
package com.healthdekho.service;

import com.healthdekho.exception.BusinessRuleException;

import java.time.LocalDate;
import java.util.regex.Pattern;

// FR-5 allows exactly three ways to search; anything else is rejected before touching the database.
public sealed interface SearchCriteria {

    Pattern POLICY_NUMBER = Pattern.compile("^[A-Z0-9-]{3,30}$");
    Pattern NAME = Pattern.compile("^[\\p{L} .'-]{2,100}$");

    record ByPolicyNumber(String policyNumber) implements SearchCriteria {
    }

    record ByCustomerName(String customerName) implements SearchCriteria {
    }

    record ByPolicyNumberAndDateOfBirth(String policyNumber, LocalDate dateOfBirth) implements SearchCriteria {
    }

    static SearchCriteria of(String policyNumber, String customerName, LocalDate dateOfBirth) {
        String number = blankToNull(policyNumber);
        String name = blankToNull(customerName);

        if (number == null && name == null && dateOfBirth == null) {
            throw new BusinessRuleException(
                    "Enter a policy number, a customer name, or a policy number with date of birth");
        }
        if (number != null && name != null) {
            throw new BusinessRuleException("Search by policy number or by customer name, not both");
        }
        if (dateOfBirth != null && number == null) {
            throw new BusinessRuleException("Date of birth can only be used together with a policy number");
        }

        if (number != null) {
            String normalized = number.toUpperCase();
            if (!POLICY_NUMBER.matcher(normalized).matches()) {
                throw new BusinessRuleException("Policy number may contain only letters, digits and '-' (3-30 chars)");
            }
            return dateOfBirth == null
                    ? new ByPolicyNumber(normalized)
                    : new ByPolicyNumberAndDateOfBirth(normalized, dateOfBirth);
        }

        // also keeps LIKE wildcards (% and _) out of the query
        if (!NAME.matcher(name).matches()) {
            throw new BusinessRuleException("Customer name must be 2-100 letters (spaces, '.', ''' and '-' allowed)");
        }
        return new ByCustomerName(name);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
