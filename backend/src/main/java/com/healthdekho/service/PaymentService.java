// ============================================================================
// OPTIONAL: FR-4 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: pays installments: ownership check, row lock, partial payment, 10% late penalty, receipts, FULLY_PAID switch.
// Only FR-4 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-4" (search the project for it). Theory and flow: docs/steps/step7.readme.md
// ============================================================================
package com.healthdekho.service;

import com.healthdekho.dto.payment.InstallmentPaymentRequest;
import com.healthdekho.dto.payment.PaymentReceipt;
import com.healthdekho.dto.payment.PaymentRequest;
import com.healthdekho.dto.payment.PaymentResponse;
import com.healthdekho.entity.CustomerPolicy;
import com.healthdekho.entity.PremiumInstallment;
import com.healthdekho.entity.PremiumPayment;
import com.healthdekho.entity.enums.PaymentMethod;
import com.healthdekho.entity.enums.PaymentStatus;
import com.healthdekho.entity.enums.PolicyStatus;
import com.healthdekho.exception.BusinessRuleException;
import com.healthdekho.exception.ConflictException;
import com.healthdekho.exception.ResourceNotFoundException;
import com.healthdekho.repository.PolicyPaymentTotals;
import com.healthdekho.repository.PremiumInstallmentRepository;
import com.healthdekho.repository.PremiumPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PolicyService policyService;
    private final PremiumInstallmentRepository installmentRepository;
    private final PremiumPaymentRepository paymentRepository;
    private final Clock clock;

    // All selected installments succeed together or none do: one bad amount rolls back the whole request
    @Transactional
    public PaymentResponse pay(String policyNumber, Long customerId, PaymentRequest request) {
        CustomerPolicy policy = policyService.getOwnedPolicy(policyNumber, customerId);
        ensureAcceptsPayments(policy);

        Map<Long, BigDecimal> amountByInstallment = toAmountMap(request.installments());
        List<PremiumInstallment> installments =
                installmentRepository.findForUpdate(policy.getId(), amountByInstallment.keySet());
        if (installments.size() != amountByInstallment.size()) {
            List<Long> found = installments.stream().map(PremiumInstallment::getId).toList();
            List<Long> missing = amountByInstallment.keySet().stream().filter(id -> !found.contains(id)).toList();
            throw new ResourceNotFoundException("Installment(s) %s not found on policy %s"
                    .formatted(missing, policy.getPolicyNumber()));
        }

        LocalDate today = LocalDate.now(clock);
        List<PaymentReceipt> receipts = installments.stream()
                .map(installment -> applyPayment(installment, amountByInstallment.get(installment.getId()),
                        request.paymentMethod(), today))
                .toList();

        // a policy is fully paid only when every installment is PAID
        if (installmentRepository.countByPolicy_IdAndPaymentStatusNot(policy.getId(), PaymentStatus.PAID) == 0) {
            policy.setStatus(PolicyStatus.FULLY_PAID);
        }

        PolicyPaymentTotals totals = installmentRepository
                .sumByPolicyIds(List.of(policy.getId()), PaymentStatus.PAID, today)
                .getFirst();
        BigDecimal paidNow = receipts.stream().map(PaymentReceipt::amountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Customer {} paid {} on policy {} ({} installment(s))", customerId, paidNow,
                policy.getPolicyNumber(), receipts.size());
        return new PaymentResponse(
                policy.getPolicyNumber(),
                receipts,
                paidNow,
                totals.premiumPaid(),
                policy.getTotalPremiumPayable().subtract(totals.premiumPaid()),
                policy.getStatus(),
                "Payment of %s recorded for %d installment(s)".formatted(paidNow, receipts.size()));
    }

    private void ensureAcceptsPayments(CustomerPolicy policy) {
        switch (policy.getStatus()) {
            case ACTIVE -> { }
            case FULLY_PAID -> throw new ConflictException(
                    "Policy %s is already fully paid".formatted(policy.getPolicyNumber()));
            case LAPSED, MATURED, CANCELLED -> throw new BusinessRuleException(
                    "Policy %s is %s and no longer accepts payments".formatted(policy.getPolicyNumber(),
                            policy.getStatus().name().toLowerCase(Locale.ROOT)));
        }
    }

    private Map<Long, BigDecimal> toAmountMap(List<InstallmentPaymentRequest> lines) {
        return lines.stream().collect(Collectors.toMap(
                InstallmentPaymentRequest::installmentId,
                line -> line.amount().setScale(2, RoundingMode.HALF_UP),
                (first, second) -> {
                    throw new BusinessRuleException("The same installment was selected more than once");
                },
                LinkedHashMap::new));
    }

    private PaymentReceipt applyPayment(PremiumInstallment installment, BigDecimal amount,
                                        PaymentMethod method, LocalDate today) {
        int number = installment.getInstallmentNumber();
        if (PremiumRules.isPaid(installment)) {
            throw new ConflictException("Installment #%d is already paid".formatted(number));
        }

        BigDecimal outstanding = PremiumRules.outstandingPremium(installment);
        BigDecimal penalty = PremiumRules.penaltyIfPaidOn(installment, today);
        if (penalty.signum() > 0) {
            BigDecimal due = outstanding.add(penalty);
            if (amount.compareTo(due) != 0) {
                throw new BusinessRuleException(("Installment #%d is past its grace period (%s). Pay the full amount "
                        + "due %s (outstanding premium %s + 10%% penalty %s)")
                        .formatted(number, installment.getGracePeriodEndDate(), due, outstanding, penalty));
            }
        } else if (amount.compareTo(outstanding) > 0) {
            throw new BusinessRuleException("Amount %s exceeds the outstanding premium %s for installment #%d"
                    .formatted(amount, outstanding, number));
        }

        installment.setPaidAmount(installment.getPaidAmount().add(amount));
        BigDecimal premiumPaid = penalty.signum() > 0
                ? installment.getScheduledAmount()
                : installment.getPaidAmount();
        installment.setPaymentStatus(PremiumRules.statusAfterPayment(premiumPaid, installment.getScheduledAmount()));

        PremiumPayment payment = new PremiumPayment();
        payment.setInstallment(installment);
        payment.setTransactionReference(newTransactionReference());
        payment.setPaymentDate(today);
        payment.setAmountPaid(amount);
        payment.setPenaltyAmount(penalty);
        payment.setPaymentMethod(method);
        paymentRepository.save(payment);

        return new PaymentReceipt(payment.getTransactionReference(), installment.getId(), number, today,
                amount, penalty, installment.getPaymentStatus());
    }

    private static String newTransactionReference() {
        return "TXN-HD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }
}
