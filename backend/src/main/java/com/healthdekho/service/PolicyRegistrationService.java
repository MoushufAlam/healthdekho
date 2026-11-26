package com.healthdekho.service;

import com.healthdekho.dto.customer.CustomerProfileResponse;
import com.healthdekho.dto.policy.PolicyRegistrationRequest;
import com.healthdekho.dto.policy.PolicyRegistrationResponse;
import com.healthdekho.dto.policy.PolicySummaryResponse;
import com.healthdekho.dto.policy.RegistrationPreviewResponse;
import com.healthdekho.dto.product.ProductResponse;
import com.healthdekho.entity.AiRecommendationItem;
import com.healthdekho.entity.Customer;
import com.healthdekho.entity.CustomerPolicy;
import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.entity.PremiumInstallment;
import com.healthdekho.entity.enums.PolicyStatus;
import com.healthdekho.exception.BusinessRuleException;
import com.healthdekho.exception.ConflictException;
import com.healthdekho.exception.ResourceNotFoundException;
import com.healthdekho.repository.AiRecommendationItemRepository;
import com.healthdekho.repository.CustomerPolicyRepository;
import com.healthdekho.repository.InsuranceProductRepository;
import com.healthdekho.repository.PremiumInstallmentRepository;
import com.healthdekho.service.PremiumScheduleCalculator.PremiumPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyRegistrationService {

    private final CustomerService customerService;
    private final InsuranceProductRepository productRepository;
    private final CustomerPolicyRepository policyRepository;
    private final PremiumInstallmentRepository installmentRepository;
    private final AiRecommendationItemRepository recommendationItemRepository;
    private final PremiumScheduleCalculator scheduleCalculator;
    private final PolicyNumberGenerator policyNumberGenerator;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RegistrationPreviewResponse preview(Long customerId, Long productId, LocalDate requestedStartDate) {
        LocalDate today = LocalDate.now(clock);
        Customer customer = customerService.getCustomer(customerId);
        InsuranceProduct product = findProduct(productId);
        LocalDate startDate = requestedStartDate != null ? requestedStartDate : today;

        Optional<String> problem = policyRepository.existsByCustomer_IdAndProduct_Id(customerId, productId)
                ? Optional.of("You already have a policy for %s".formatted(product.getPolicyName()))
                : eligibilityProblem(customer, product, startDate, today);

        PremiumPlan plan = scheduleCalculator.plan(product, startDate);
        return new RegistrationPreviewResponse(
                CustomerProfileResponse.from(customer, today),
                ProductResponse.from(product),
                plan.startDate(),
                plan.maturityDate(),
                plan.numberOfInstallments(),
                plan.totalPremiumPayable(),
                plan.firstDueDate(),
                plan.lastDueDate(),
                problem.isEmpty(),
                problem.orElse("You are eligible for this policy"));
    }

    // One transaction: the policy and all its installments are saved together, or nothing is saved
    @Transactional
    public PolicyRegistrationResponse register(Long customerId, PolicyRegistrationRequest request) {
        LocalDate today = LocalDate.now(clock);
        Customer customer = customerService.getCustomer(customerId);
        InsuranceProduct product = findProduct(request.productId());
        LocalDate startDate = request.startDate() != null ? request.startDate() : today;

        if (policyRepository.existsByCustomer_IdAndProduct_Id(customerId, product.getId())) {
            throw new ConflictException("You already have a policy for %s (%s)"
                    .formatted(product.getPolicyName(), product.getProductCode()));
        }
        eligibilityProblem(customer, product, startDate, today).ifPresent(problem -> {
            throw new BusinessRuleException(problem);
        });

        PremiumPlan plan = scheduleCalculator.plan(product, startDate);
        if (!plan.maturityDate().isAfter(plan.startDate())) {
            throw new BusinessRuleException("Maturity date must be after the start date");
        }

        CustomerPolicy policy = new CustomerPolicy();
        policy.setPolicyNumber(policyNumberGenerator.next(today));
        policy.setCustomer(customer);
        policy.setProduct(product);
        policy.setStartDate(plan.startDate());
        policy.setMaturityDate(plan.maturityDate());
        // copied from the product so later catalog changes don't alter a policy that was already sold
        policy.setPremiumFrequency(product.getPremiumFrequency());
        policy.setPremiumAmount(product.getPremiumAmount());
        policy.setSumInsured(product.getSumInsured());
        policy.setTotalPremiumPayable(plan.totalPremiumPayable());
        policy.setStatus(PolicyStatus.ACTIVE);
        policyRepository.save(policy);

        List<PremiumInstallment> installments = plan.installments().stream()
                .map(scheduled -> {
                    PremiumInstallment installment = new PremiumInstallment();
                    installment.setPolicy(policy);
                    installment.setInstallmentNumber(scheduled.number());
                    installment.setDueDate(scheduled.dueDate());
                    installment.setGracePeriodEndDate(scheduled.gracePeriodEndDate());
                    installment.setScheduledAmount(scheduled.amount());
                    return installment;
                })
                .toList();
        installmentRepository.saveAll(installments);

        if (request.recommendationItemId() != null) {
            markRecommendationSelected(request.recommendationItemId(), customerId, product.getId());
        }

        log.info("Customer {} registered policy {} for product {}", customerId, policy.getPolicyNumber(),
                product.getProductCode());
        return new PolicyRegistrationResponse(
                policy.getPolicyNumber(),
                "Policy %s registered successfully".formatted(policy.getPolicyNumber()),
                PolicySummaryResponse.from(policy),
                plan.numberOfInstallments(),
                plan.totalPremiumPayable(),
                plan.firstDueDate());
    }

    private InsuranceProduct findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product %d not found".formatted(productId)));
    }

    private Optional<String> eligibilityProblem(Customer customer, InsuranceProduct product,
                                                LocalDate startDate, LocalDate today) {
        if (!Boolean.TRUE.equals(product.getActive())) {
            return Optional.of("Product %s is not available for registration".formatted(product.getProductCode()));
        }
        if (product.getPremiumAmount().signum() <= 0 || product.getSumInsured().signum() <= 0
                || product.getTermMonths() <= 0) {
            return Optional.of("Product %s has invalid premium, sum insured or term".formatted(product.getProductCode()));
        }
        if (startDate.isBefore(today)) {
            return Optional.of("Policy start date cannot be in the past");
        }
        if (customer.getDateOfBirth().isAfter(today)) {
            return Optional.of("Date of birth cannot be in the future");
        }
        // entry age is checked on the day cover starts
        int age = AgeCalculator.ageOn(customer.getDateOfBirth(), startDate);
        if (age < product.getMinEntryAge() || age > product.getMaxEntryAge()) {
            return Optional.of("Your age on the start date (%d) is outside the entry age range %d-%d for %s"
                    .formatted(age, product.getMinEntryAge(), product.getMaxEntryAge(), product.getPolicyName()));
        }
        return Optional.empty();
    }

    private void markRecommendationSelected(Long itemId, Long customerId, Long productId) {
        AiRecommendationItem item = recommendationItemRepository.findOwnedItem(itemId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation %d not found".formatted(itemId)));
        if (!item.getProduct().getId().equals(productId)) {
            throw new BusinessRuleException("Recommendation %d is for a different product".formatted(itemId));
        }
        item.setSelectedForRegistration(true);
    }
}
