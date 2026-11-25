package com.healthdekho.repository;

import com.healthdekho.entity.PremiumPayment;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for premium payments rows are immutable. */
public interface PremiumPaymentRepository extends JpaRepository<PremiumPayment, Long> {
}
