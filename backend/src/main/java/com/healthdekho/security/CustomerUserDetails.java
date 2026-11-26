package com.healthdekho.security;

import com.healthdekho.entity.Customer;
import com.healthdekho.entity.enums.CustomerStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record CustomerUserDetails(Long id, String email, String passwordHash, boolean active)
        implements UserDetails {

    public static CustomerUserDetails from(Customer customer) {
        return new CustomerUserDetails(
                customer.getId(),
                customer.getEmail(),
                customer.getPasswordHash(),
                customer.getStatus() == CustomerStatus.ACTIVE);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    // INACTIVE / LOCKED customers fail login with DisabledException
    @Override
    public boolean isEnabled() {
        return active;
    }
}
