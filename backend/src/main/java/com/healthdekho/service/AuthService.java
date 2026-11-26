package com.healthdekho.service;

import com.healthdekho.dto.auth.LoginRequest;
import com.healthdekho.dto.auth.LoginResponse;
import com.healthdekho.exception.AuthenticationFailedException;
import com.healthdekho.security.CustomerUserDetails;
import com.healthdekho.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final CustomerService customerService;

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (DisabledException ex) {
            throw new AuthenticationFailedException("This account is not active. Please contact support");
        } catch (AuthenticationException ex) {
            // same message for unknown email and wrong password, so attackers can't probe which emails exist
            log.info("Failed login for {}", email);
            throw new AuthenticationFailedException("Invalid email or password");
        }

        if (!(authentication.getPrincipal() instanceof CustomerUserDetails customer)) {
            throw new IllegalStateException("Unexpected principal type: " + authentication.getPrincipal());
        }
        JwtTokenService.IssuedToken token = jwtTokenService.issue(customer);
        return new LoginResponse(token.value(), "Bearer", token.expiresAt(),
                customerService.getProfile(customer.id()));
    }
}
