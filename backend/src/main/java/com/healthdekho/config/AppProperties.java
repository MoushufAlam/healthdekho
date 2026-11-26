package com.healthdekho.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Ai ai) {

    public record Jwt(String secret, String issuer, Duration ttl) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Ai(Duration timeout, int maxRecommendations, String promptVersion) {
    }
}
