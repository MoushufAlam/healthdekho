package com.healthdekho.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentCustomer {

    private CurrentCustomer() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
