package com.healthdekho.service;

import com.healthdekho.repository.CustomerPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class PolicyNumberGenerator {

    private final CustomerPolicyRepository policyRepository;

    // HDP-<year>-<4-digit sequence>. Two registrations in the same instant could pick the same number;
    // the unique constraint on policy_number rejects the second one (409) instead of saving a duplicate.
    public String next(LocalDate today) {
        String prefix = "HDP-%d-".formatted(today.getYear());
        int nextSequence = policyRepository.findMaxPolicyNumberStartingWith(prefix)
                .map(last -> Integer.parseInt(last.substring(prefix.length())) + 1)
                .orElse(1);
        return prefix + "%04d".formatted(nextSequence);
    }
}
