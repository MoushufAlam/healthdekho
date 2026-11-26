package com.healthdekho.service;

import com.healthdekho.dto.payment.PaymentHistoryResponse;
import com.healthdekho.dto.policy.DashboardResponse;
import com.healthdekho.dto.policy.InstallmentResponse;
import com.healthdekho.dto.policy.PolicyDetailResponse;
import com.healthdekho.dto.policy.PolicySummaryResponse;
import com.healthdekho.entity.CustomerPolicy;
import com.healthdekho.entity.PremiumInstallment;
import com.healthdekho.entity.enums.PaymentStatus;
import com.healthdekho.exception.ResourceNotFoundException;
import com.healthdekho.repository.CustomerPolicyRepository;
import com.healthdekho.repository.PremiumInstallmentRepository;
import com.healthdekho.repository.PremiumPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

    private final CustomerPolicyRepository policyRepository;
    private final PremiumInstallmentRepository installmentRepository;
    private final PremiumPaymentRepository paymentRepository;
    private final CustomerService customerService;
    private final Clock clock;

    public DashboardResponse getDashboard(Long customerId) {
        String customerName = customerService.getCustomer(customerId).getFullName();
        List<PolicySummaryResponse> policies = policyRepository.findAllByCustomerIdWithProduct(customerId).stream()
                .map(PolicySummaryResponse::from)
                .toList();
        String message = policies.isEmpty()
                ? "You have not registered any insurance policy yet. Explore the available products to get started."
                : null;
        return new DashboardResponse(customerName, policies.size(), policies, message);
    }

    // ============================================================================
    // OPTIONAL: FR-4 (not evaluated). Everything below getDashboard() serves only the FR-4
    // policy lookup / schedule / payment history. FR-1B (dashboard) only needs getDashboard().
    // If you remove FR-4, also drop the installmentRepository, paymentRepository and clock fields
    // and the unused imports. Theory and flow: docs/steps/step7.readme.md
    // ============================================================================

    // 404 for both "doesn't exist" and "belongs to someone else"
    public CustomerPolicy getOwnedPolicy(String policyNumber, Long customerId) {
        return policyRepository.findOwnedPolicy(normalize(policyNumber), customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy %s not found".formatted(policyNumber)));
    }

    public PolicyDetailResponse getPolicyDetail(String policyNumber, Long customerId) {
        CustomerPolicy policy = getOwnedPolicy(policyNumber, customerId);
        List<PremiumInstallment> installments =
                installmentRepository.findByPolicy_IdOrderByInstallmentNumberAsc(policy.getId());
        LocalDate today = LocalDate.now(clock);

        BigDecimal premiumPaid = sum(installments, PremiumRules::premiumPaid);
        BigDecimal penaltyPaid = sum(installments, PremiumRules::penaltyPaid);
        Map<PaymentStatus, Long> statusCounts = installments.stream()
                .collect(Collectors.groupingBy(
                        installment -> PremiumRules.effectiveStatus(installment, today),
                        () -> new EnumMap<>(PaymentStatus.class),
                        Collectors.counting()));

        return new PolicyDetailResponse(
                policy.getPolicyNumber(),
                policy.getCustomer().getFullName(),
                policy.getProduct().getPolicyName(),
                policy.getProduct().getPolicyType(),
                policy.getProduct().getTermMonths(),
                policy.getStartDate(),
                policy.getMaturityDate(),
                policy.getPremiumFrequency(),
                policy.getPremiumAmount(),
                policy.getTotalPremiumPayable(),
                premiumPaid,
                policy.getTotalPremiumPayable().subtract(premiumPaid),
                penaltyPaid,
                policy.getStatus(),
                statusCounts);
    }

    public List<InstallmentResponse> getInstallments(String policyNumber, Long customerId) {
        CustomerPolicy policy = getOwnedPolicy(policyNumber, customerId);
        LocalDate today = LocalDate.now(clock);
        return installmentRepository.findByPolicy_IdOrderByInstallmentNumberAsc(policy.getId()).stream()
                .map(installment -> toInstallmentResponse(installment, today))
                .toList();
    }

    public List<PaymentHistoryResponse> getPaymentHistory(String policyNumber, Long customerId) {
        CustomerPolicy policy = getOwnedPolicy(policyNumber, customerId);
        return paymentRepository.findHistoryByPolicyId(policy.getId()).stream()
                .map(PaymentHistoryResponse::from)
                .toList();
    }

    static String normalize(String policyNumber) {
        return policyNumber == null ? null : policyNumber.trim().toUpperCase();
    }

    private InstallmentResponse toInstallmentResponse(PremiumInstallment installment, LocalDate today) {
        PaymentStatus status = PremiumRules.effectiveStatus(installment, today);
        return new InstallmentResponse(
                installment.getId(),
                installment.getInstallmentNumber(),
                YearMonth.from(installment.getDueDate()),
                installment.getScheduledAmount(),
                installment.getDueDate(),
                installment.getGracePeriodEndDate(),
                installment.getPaidAmount(),
                PremiumRules.outstandingPremium(installment),
                PremiumRules.penaltyIfPaidOn(installment, today),
                PremiumRules.amountDueOn(installment, today),
                status,
                status != PaymentStatus.PAID);
    }

    private static BigDecimal sum(List<PremiumInstallment> installments,
                                  Function<PremiumInstallment, BigDecimal> amount) {
        return installments.stream().map(amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
