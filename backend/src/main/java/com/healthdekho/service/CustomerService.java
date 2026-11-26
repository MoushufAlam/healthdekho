package com.healthdekho.service;

import com.healthdekho.dto.customer.CustomerProfileResponse;
import com.healthdekho.entity.Customer;
import com.healthdekho.exception.ResourceNotFoundException;
import com.healthdekho.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final Clock clock;

    public Customer getCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    public CustomerProfileResponse getProfile(Long customerId) {
        return CustomerProfileResponse.from(getCustomer(customerId), LocalDate.now(clock));
    }
}
