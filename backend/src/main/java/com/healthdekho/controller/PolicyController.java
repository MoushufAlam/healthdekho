package com.healthdekho.controller;

import com.healthdekho.dto.common.PageResponse;
import com.healthdekho.dto.payment.PaymentHistoryResponse;
import com.healthdekho.dto.payment.PaymentRequest;
import com.healthdekho.dto.payment.PaymentResponse;
import com.healthdekho.dto.policy.InstallmentResponse;
import com.healthdekho.dto.policy.PolicyDetailResponse;
import com.healthdekho.dto.policy.PolicyRegistrationRequest;
import com.healthdekho.dto.policy.PolicyRegistrationResponse;
import com.healthdekho.dto.policy.RegistrationPreviewResponse;
import com.healthdekho.dto.search.PolicySearchResultResponse;
import com.healthdekho.security.CurrentCustomer;
import com.healthdekho.service.PaymentService;
import com.healthdekho.service.PolicyRegistrationService;
import com.healthdekho.service.PolicySearchService;
import com.healthdekho.service.PolicyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

// The customer id always comes from the JWT; no endpoint accepts it as a parameter.
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyRegistrationService registrationService;
    private final PaymentService paymentService;        // OPTIONAL: FR-4
    private final PolicySearchService searchService;    // OPTIONAL: FR-5

    // ---- FR-3 registration ----

    @GetMapping("/preview")
    public RegistrationPreviewResponse preview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @Positive Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        return registrationService.preview(CurrentCustomer.id(jwt), productId, startDate);
    }

    @PostMapping
    public ResponseEntity<PolicyRegistrationResponse> register(@AuthenticationPrincipal Jwt jwt,
                                                               @Valid @RequestBody PolicyRegistrationRequest request) {
        PolicyRegistrationResponse response = registrationService.register(CurrentCustomer.id(jwt), request);
        return ResponseEntity.created(URI.create("/api/policies/" + response.policyNumber())).body(response);
    }

    // ============================================================================
    // OPTIONAL: FR-5 Policy Search (not evaluated). Remove this endpoint together with
    // the searchService field and its imports. Theory and flow: docs/steps/step8.readme.md
    // ============================================================================
    // literal path "/search" wins over /{policyNumber}

    @GetMapping("/search")
    public PageResponse<PolicySearchResultResponse> search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String policyNumber,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return searchService.search(CurrentCustomer.id(jwt), policyNumber, customerName, dateOfBirth, page, size);
    }

    // ============================================================================
    // OPTIONAL: FR-4 Premium Payment (not evaluated): lookup, schedule, pay, history.
    // Remove these 4 endpoints together with the paymentService field and their imports.
    // Theory and flow: docs/steps/step7.readme.md
    // ============================================================================

    @GetMapping("/{policyNumber}")
    public PolicyDetailResponse detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String policyNumber) {
        return policyService.getPolicyDetail(policyNumber, CurrentCustomer.id(jwt));
    }

    @GetMapping("/{policyNumber}/installments")
    public List<InstallmentResponse> installments(@AuthenticationPrincipal Jwt jwt, @PathVariable String policyNumber) {
        return policyService.getInstallments(policyNumber, CurrentCustomer.id(jwt));
    }

    @PostMapping("/{policyNumber}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable String policyNumber,
                               @Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(policyNumber, CurrentCustomer.id(jwt), request);
    }

    @GetMapping("/{policyNumber}/payments")
    public List<PaymentHistoryResponse> paymentHistory(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable String policyNumber) {
        return policyService.getPaymentHistory(policyNumber, CurrentCustomer.id(jwt));
    }
}
