package com.healthdekho.repository;

import com.healthdekho.entity.AiRecommendationItem;
import com.healthdekho.entity.Customer;
import com.healthdekho.entity.CustomerPolicy;
import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.entity.enums.CustomerStatus;
import com.healthdekho.entity.enums.PolicyType;
import com.healthdekho.entity.enums.PremiumFrequency;
import com.healthdekho.entity.enums.Suitability;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// Runs against the local dev DB and relies on the V2 seed data. Each test is rolled back.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryMappingTest {

    @Autowired private CustomerRepository customerRepository;
    @Autowired private InsuranceProductRepository productRepository;
    @Autowired private CustomerPolicyRepository policyRepository;
    @Autowired private PremiumInstallmentRepository installmentRepository;
    @Autowired private PremiumPaymentRepository paymentRepository;
    @Autowired private AiRecommendationRequestRepository aiRequestRepository;
    @Autowired private AiRecommendationItemRepository aiItemRepository;

    @Test
    void findByEmail_returnsSeededCustomer() {
        Customer aarav = customerRepository.findByEmail("aarav.sharma@example.com").orElseThrow();

        assertThat(aarav.getId()).isEqualTo(1001L);
        assertThat(aarav.getFullName()).isEqualTo("Aarav Sharma");
        assertThat(aarav.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 4, 15));
        assertThat(aarav.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(aarav.getPasswordHash()).startsWith("{bcrypt}$2a$10$");
    }

    @Test
    void findByEmail_unknownEmail_returnsEmptyOptional() {
        assertThat(customerRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void product_enumsAndMoneyAreMapped() {
        InsuranceProduct savings = productRepository.findById(203L).orElseThrow();

        assertThat(savings.getPolicyType()).isEqualTo(PolicyType.SAVINGS);
        assertThat(savings.getPremiumFrequency()).isEqualTo(PremiumFrequency.QUARTERLY);
        assertThat(savings.getPremiumFrequency().getMonths()).isEqualTo(3);
        // compare by value: 7500 and 7500.00 are not equals() as BigDecimal
        assertThat(savings.getPremiumAmount()).isEqualByComparingTo("7500.00");
    }

    @Test
    void policy_manyToOneRelationsLoad() {
        CustomerPolicy policy = policyRepository.findById(3001L).orElseThrow();

        assertThat(policy.getCustomer().getFullName()).isEqualTo("Aarav Sharma");
        assertThat(policy.getProduct().getProductCode()).isEqualTo("HD-TERM-001");
        assertThat(policy.getTotalPremiumPayable()).isEqualByComparingTo(new BigDecimal("300000"));
    }

    @Test
    void aiItem_linksRequestAndProduct() {
        AiRecommendationItem item = aiItemRepository.findById(7001L).orElseThrow();

        assertThat(item.getRequest().getCustomer().getId()).isEqualTo(1001L);
        assertThat(item.getProduct().getId()).isEqualTo(201L);
        assertThat(item.getSuitability()).isEqualTo(Suitability.HIGH);
    }

    @Test
    void seedRowCounts() {
        assertThat(customerRepository.count()).isEqualTo(3);
        assertThat(productRepository.count()).isEqualTo(3);
        assertThat(policyRepository.count()).isEqualTo(2);
        assertThat(installmentRepository.count()).isEqualTo(241);
        assertThat(paymentRepository.count()).isEqualTo(3);
        assertThat(aiRequestRepository.count()).isEqualTo(2);
        assertThat(aiItemRepository.count()).isEqualTo(3);
    }
}
