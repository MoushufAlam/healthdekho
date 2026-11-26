package com.healthdekho.ai;

import com.healthdekho.ai.AiSuggestion.AiSuggestionList;
import com.healthdekho.config.AppProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class GeminiRecommendationClient implements RecommendationAiClient {

    static final String NOT_CONFIGURED = "not-configured";

    private final ChatClient chatClient;
    private final ExecutorService aiExecutor;
    private final Duration timeout;
    private final String apiKey;
    private final String model;

    public GeminiRecommendationClient(ChatClient.Builder chatClientBuilder,
                                      ExecutorService aiExecutor,
                                      AppProperties properties,
                                      @Value("${spring.ai.google.genai.api-key}") String apiKey,
                                      @Value("${spring.ai.google.genai.chat.options.model}") String model) {
        this.chatClient = chatClientBuilder.build();
        this.aiExecutor = aiExecutor;
        this.timeout = properties.ai().timeout();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public List<AiSuggestion> suggest(AiRecommendationInput input) {
        if (apiKey == null || apiKey.isBlank() || NOT_CONFIGURED.equals(apiKey)) {
            throw new AiUnavailableException("GEMINI_API_KEY is not configured");
        }
        // run on a virtual thread so we can stop waiting after the timeout instead of hanging the request
        CompletableFuture<AiSuggestionList> call = CompletableFuture.supplyAsync(() -> chatClient.prompt()
                .system(RecommendationPrompt.system(input))
                .user(RecommendationPrompt.user(input))
                .call()
                .entity(AiSuggestionList.class), aiExecutor);
        try {
            AiSuggestionList answer = call.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return answer == null || answer.recommendations() == null ? List.of() : answer.recommendations();
        } catch (TimeoutException ex) {
            call.cancel(true);
            throw new AiUnavailableException("AI did not answer within " + timeout.toSeconds() + "s", ex);
        } catch (ExecutionException ex) {
            throw new AiUnavailableException("AI call failed: " + ex.getCause().getMessage(), ex.getCause());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("AI call was interrupted", ex);
        }
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public String modelVersion() {
        return "google-genai";
    }
}
