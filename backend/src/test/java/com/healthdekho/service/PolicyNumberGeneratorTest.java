package com.healthdekho.service;

import com.healthdekho.repository.CustomerPolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PNUM - policy number generation")
class PolicyNumberGeneratorTest {

    @Mock
    private CustomerPolicyRepository policyRepository;

    @InjectMocks
    private PolicyNumberGenerator generator;

    @Test
    @DisplayName("PNUM-01 continues the sequence of the current year")
    void continuesSequence() {
        when(policyRepository.findMaxPolicyNumberStartingWith("HDP-2026-")).thenReturn(Optional.of("HDP-2026-0041"));

        assertThat(generator.next(LocalDate.of(2026, 11, 26))).isEqualTo("HDP-2026-0042");
    }

    @Test
    @DisplayName("PNUM-02 first policy of a new year starts at 0001")
    void newYear() {
        when(policyRepository.findMaxPolicyNumberStartingWith("HDP-2027-")).thenReturn(Optional.empty());

        assertThat(generator.next(LocalDate.of(2027, 1, 1))).isEqualTo("HDP-2027-0001");
    }
}
