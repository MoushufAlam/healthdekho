package com.healthdekho.repository;

import com.healthdekho.entity.PremiumInstallment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PremiumInstallmentRepository extends JpaRepository<PremiumInstallment, Long> {
}
