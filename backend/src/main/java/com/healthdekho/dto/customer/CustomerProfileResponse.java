package com.healthdekho.dto.customer;

import com.healthdekho.entity.Customer;
import com.healthdekho.service.AgeCalculator;

import java.time.LocalDate;

public record CustomerProfileResponse(
        Long customerId,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        int age) {

    public static CustomerProfileResponse from(Customer customer, LocalDate today) {
        return new CustomerProfileResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getDateOfBirth(),
                AgeCalculator.ageOn(customer.getDateOfBirth(), today));
    }
}
