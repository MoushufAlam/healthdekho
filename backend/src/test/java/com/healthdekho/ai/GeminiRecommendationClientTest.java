package com.healthdekho.ai;

import com.healthdekho.ai.AiSuggestion.AiSuggestionList;
import com.healthdekho.config.AppProperties;
import com.healthdekho.dto.product.ProductResponse;
import com.healthdekho.entity.enums.PolicyType;
import com.healthdekho.entity.enums.PremiumFrequency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AI-C - Gemini client wrapper (no network)")
class GeminiRecommendationClientTest {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);

    private final AiRecommendationInput input = new AiRecommendationInput(36, "Family protection", "20 years",
            PolicyType.TERM,
            List.of(new ProductResponse(201L, "HD-TERM-001", "HealthDekho Secure Term", PolicyType.TERM, 240,
                    new BigDecimal("5000000.00"), PremiumFrequency.MONTHLY, new BigDecimal("1250.00"), 18, 60)),
            3);

    @AfterEach
    void shutdown() {
        executor.close();
    }

    private GeminiRecommendationClient client(String apiKey, Duration timeout) {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);
        AppProperties properties = new AppProperties(null, null, new AppProperties.Ai(timeout, 3, "rec-v1"));
        return new GeminiRecommendationClient(builder, executor, properties, apiKey, "gemini-2.5-flash");
    }

    @Test
    @DisplayName("AI-C-01 without an API key the client refuses immediately (no network call)")
    void notConfigured() {
        assertThatThrownBy(() -> client("not-configured", Duration.ofSeconds(1)).suggest(input))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("GEMINI_API_KEY");
    }

    @Test
    @DisplayName("AI-C-02 structured answer is returned as suggestions")
    void returnsSuggestions() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().entity(AiSuggestionList.class))
                .thenReturn(new AiSuggestionList(List.of(new AiSuggestion("HD-TERM-001", 1, "Protects family", "HIGH"))));

        GeminiRecommendationClient client = client("real-key", Duration.ofSeconds(5));

        assertThat(client.suggest(input)).extracting(AiSuggestion::productCode).containsExactly("HD-TERM-001");
        assertThat(client.modelName()).isEqualTo("gemini-2.5-flash");
    }

    @Test
    @DisplayName("AI-C-03 a slow model is cut off after the timeout")
    void timeout() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().entity(AiSuggestionList.class))
                .thenAnswer(invocation -> {
                    Thread.sleep(2_000);
                    return null;
                });

        assertThatThrownBy(() -> client("real-key", Duration.ofMillis(100)).suggest(input))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("did not answer");
    }

    @Test
    @DisplayName("AI-C-04 model errors are wrapped so the service can fall back")
    void modelError() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().entity(AiSuggestionList.class))
                .thenThrow(new IllegalStateException("401 API key invalid"));

        assertThatThrownBy(() -> client("real-key", Duration.ofSeconds(5)).suggest(input))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("401 API key invalid");
    }

    @Test
    @DisplayName("AI-C-05 prompt lists only the candidate products and the customer's inputs")
    void promptContent() {
        String user = RecommendationPrompt.user(input);

        assertThat(user).contains("age: 36", "Family protection", "productCode=HD-TERM-001", "premium=1250.00");
        assertThat(RecommendationPrompt.system(input)).contains("ONLY from the CANDIDATE PRODUCTS", "at most 3");
    }
}
