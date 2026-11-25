package com.healthdekho.repository;

import com.healthdekho.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // email doubles as the login username
    Optional<Customer> findByEmail(String email);
} 
