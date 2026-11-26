package com.healthdekho.dto.recommendation;

import java.util.List;

public record RecommendationResponse(
        Long recommendationRequestId,
        int calculatedAge,
        RecommendationSource source,
        String modelName,
        String message,
        List<RecommendedProductResponse> recommendations) {
}
