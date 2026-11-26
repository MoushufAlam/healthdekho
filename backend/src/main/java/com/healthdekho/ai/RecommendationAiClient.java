package com.healthdekho.ai;

import java.util.List;

// Our own small interface in front of Spring AI: the service doesn't know which model is behind it,
// and tests can replace it with a mock.
public interface RecommendationAiClient {

    /**
     * @throws AiUnavailableException when the model is not configured, times out or fails
     */
    List<AiSuggestion> suggest(AiRecommendationInput input);

    String modelName();

    String modelVersion();
}
