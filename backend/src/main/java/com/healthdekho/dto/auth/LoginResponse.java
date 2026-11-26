package com.healthdekho.dto.auth;

import com.healthdekho.dto.customer.CustomerProfileResponse;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        CustomerProfileResponse customer) {
}
