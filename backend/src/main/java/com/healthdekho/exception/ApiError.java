package com.healthdekho.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ApiError of(Clock clock, HttpStatus status, String message, String path) {
        return of(clock, status, message, path, Map.of());
    }

    public static ApiError of(Clock clock, HttpStatus status, String message, String path,
                              Map<String, String> fieldErrors) {
        return new ApiError(clock.instant(), status.value(), status.getReasonPhrase(), message, path, fieldErrors);
    }
}
