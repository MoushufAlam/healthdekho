package com.healthdekho.security;

import com.healthdekho.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return customerRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(CustomerUserDetails::from)
                .orElseThrow(() -> new UsernameNotFoundException("No customer with this email"));
    }
}
