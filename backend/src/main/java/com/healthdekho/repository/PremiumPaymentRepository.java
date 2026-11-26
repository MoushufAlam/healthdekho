package com.healthdekho.repository;

import com.healthdekho.entity.PremiumPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PremiumPaymentRepository extends JpaRepository<PremiumPayment, Long> {

    // OPTIONAL: FR-4 (not evaluated). Payment history; FR-1 to FR-3 never read payments.
    @Query("""
            select p from PremiumPayment p
            join fetch p.installment i
            where i.policy.id = :policyId
            order by p.paymentDate desc, p.id desc
            """)
    List<PremiumPayment> findHistoryByPolicyId(Long policyId);
}
