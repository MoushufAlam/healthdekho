package com.healthdekho.repository;

import com.healthdekho.entity.CustomerPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerPolicyRepository extends JpaRepository<CustomerPolicy, Long> {
}
