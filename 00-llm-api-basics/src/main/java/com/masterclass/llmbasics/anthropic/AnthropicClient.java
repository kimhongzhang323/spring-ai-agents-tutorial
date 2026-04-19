package com.masterclass.llmbasics.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.masterclass.llmbasics.common.HttpHelper;
import com.masterclass.llmbasics.common.LlmResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Minimal Anthropic Messages client.
 * Docs: https://docs.anthropic.com/en/api/messages
 *
 * Key differences from OpenAI:
 *  - Endpoint: POST https://api.anthropic.com/v1/messages
 *  - Auth uses "x-api-key" header, NOT "Authorization: Bearer"
 *  - Requires "anthropic-version" header
 *  - Body has "max_tokens" (required!) and optional top-level "system" string
 *  - Response text is at content[0].text
 */
public class AnthropicClient {

    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String apiKey;
    private final String model;

    public AnthropicClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public LlmResponse chat(String userMessage) throws Exception {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 1024,
                "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                )
        );

        String json = HttpHelper.MAPPER.writeValueAsString(body);

        // Anthropic uses x-api-key, not Authorization: Bearer
        HttpResponse<String> response = CLIENT.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(ENDPOINT))
                        .timeout(Duration.ofSeconds(60))
                        .header("Content-Type", "application/json")
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", API_VERSION)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP %d: %s".formatted(response.statusCode(), response.body()));
        }

        JsonNode root = HttpHelper.MAPPER.readTree(response.body());
        String content = root.at("/content/0/text").asText();
        int inputTokens = root.at("/usage/input_tokens").asInt();
        int outputTokens = root.at("/usage/output_tokens").asInt();

        return new LlmResponse("Anthropic", model, content, inputTokens, outputTokens);
    }
}
