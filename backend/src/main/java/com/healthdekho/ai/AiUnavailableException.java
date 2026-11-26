package com.healthdekho.ai;

// Never reaches the client: RecommendationService catches it and switches to the rule-based fallback
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
