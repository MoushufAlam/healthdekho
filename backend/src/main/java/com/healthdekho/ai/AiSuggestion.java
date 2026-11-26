package com.healthdekho.ai;

import java.util.List;

// Shape we ask the model to answer in. Values are untrusted until RecommendationService validates them.
public record AiSuggestion(String productCode, Integer rank, String reason, String suitability) {

    public record AiSuggestionList(List<AiSuggestion> recommendations) {
    }
}
