// ============================================================================
// OPTIONAL: FR-4 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: request body of POST /api/policies/{policyNumber}/payments.
// Only FR-4 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-4" (search the project for it). Theory and flow: docs/steps/step7.readme.md
// ============================================================================
package com.healthdekho.dto.payment;

import com.healthdekho.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PaymentRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @NotEmpty(message = "Select at least one installment")
        @Size(max = 24, message = "At most 24 installments can be paid at once")
        List<@Valid @NotNull InstallmentPaymentRequest> installments) {
}
