// ============================================================================
// OPTIONAL: FR-5 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: search own policies by number / name / number+DOB, paged, with payment status.
// Only FR-5 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-5" (search the project for it). Theory and flow: docs/steps/step8.readme.md
// ============================================================================
package com.healthdekho.service;

import com.healthdekho.dto.common.PageResponse;
import com.healthdekho.dto.search.PolicySearchResultResponse;
import com.healthdekho.dto.search.PremiumPaymentStatus;
import com.healthdekho.entity.CustomerPolicy;
import com.healthdekho.entity.enums.PaymentStatus;
import com.healthdekho.entity.enums.PolicyStatus;
import com.healthdekho.exception.ResourceNotFoundException;
import com.healthdekho.repository.CustomerPolicyRepository;
import com.healthdekho.repository.PolicyPaymentTotals;
import com.healthdekho.repository.PremiumInstallmentRepository;
import com.healthdekho.service.SearchCriteria.ByCustomerName;
import com.healthdekho.service.SearchCriteria.ByPolicyNumber;
import com.healthdekho.service.SearchCriteria.ByPolicyNumberAndDateOfBirth;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicySearchService {

    static final String NO_RESULTS = "No policies match the search criteria";

    private final CustomerPolicyRepository policyRepository;
    private final PremiumInstallmentRepository installmentRepository;
    private final Clock clock;

    public PageResponse<PolicySearchResultResponse> search(Long customerId, String policyNumber, String customerName,
                                                           LocalDate dateOfBirth, int page, int size) {
        SearchCriteria criteria = SearchCriteria.of(policyNumber, customerName, dateOfBirth);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));

        // every branch is scoped to the logged-in customer; a policy number that isn't theirs is a 404
        Page<CustomerPolicy> policies = switch (criteria) {
            case ByPolicyNumber(String number) -> single(
                    policyRepository.findOwnedPolicy(number, customerId),
                    "Policy %s not found".formatted(number), pageable);
            case ByPolicyNumberAndDateOfBirth(String number, LocalDate dob) -> single(
                    policyRepository.findOwnedPolicy(number, customerId)
                            .filter(policy -> policy.getCustomer().getDateOfBirth().equals(dob)),
                    "No policy %s found for date of birth %s".formatted(number, dob), pageable);
            case ByCustomerName(String name) -> policyRepository.searchOwnedByCustomerName(customerId, name, pageable);
        };

        LocalDate today = LocalDate.now(clock);
        Map<Long, PolicyPaymentTotals> totalsByPolicy = policies.isEmpty()
                ? Map.of()
                : installmentRepository
                        .sumByPolicyIds(policies.map(CustomerPolicy::getId).toList(), PaymentStatus.PAID, today)
                        .stream()
                        .collect(Collectors.toMap(PolicyPaymentTotals::policyId, Function.identity()));

        Page<PolicySearchResultResponse> results =
                policies.map(policy -> toResult(policy, totalsByPolicy.get(policy.getId())));
        return PageResponse.from(results, NO_RESULTS);
    }

    private Page<CustomerPolicy> single(Optional<CustomerPolicy> policy, String notFoundMessage, Pageable pageable) {
        CustomerPolicy found = policy.orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
        return new PageImpl<>(List.of(found), pageable, 1);
    }

    private PolicySearchResultResponse toResult(CustomerPolicy policy, PolicyPaymentTotals totals) {
        BigDecimal premiumPaid = totals == null ? BigDecimal.ZERO : totals.premiumPaid();
        long overdue = totals == null ? 0 : totals.overdueInstallments();
        BigDecimal remaining = policy.getTotalPremiumPayable().subtract(premiumPaid);

        PremiumPaymentStatus paymentStatus;
        if (policy.getStatus() == PolicyStatus.FULLY_PAID || remaining.signum() <= 0) {
            paymentStatus = PremiumPaymentStatus.FULLY_PAID;
        } else if (overdue > 0) {
            paymentStatus = PremiumPaymentStatus.OVERDUE;
        } else {
            paymentStatus = PremiumPaymentStatus.ON_TRACK;
        }

        return new PolicySearchResultResponse(
                policy.getPolicyNumber(),
                policy.getCustomer().getFullName(),
                policy.getCustomer().getDateOfBirth(),
                policy.getProduct().getPolicyName(),
                policy.getProduct().getPolicyType(),
                policy.getStartDate(),
                policy.getMaturityDate(),
                policy.getTotalPremiumPayable(),
                paymentStatus,
                premiumPaid,
                remaining);
    }
}
