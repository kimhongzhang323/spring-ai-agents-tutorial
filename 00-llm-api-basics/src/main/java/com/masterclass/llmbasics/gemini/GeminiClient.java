package com.masterclass.llmbasics.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.masterclass.llmbasics.common.HttpHelper;
import com.masterclass.llmbasics.common.LlmResponse;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini client via Generative Language REST API.
 * Docs: https://ai.google.dev/api/generate-content
 *
 * Key differences from OpenAI:
 *  - Endpoint includes model name in the URL path
 *  - API key passed as query param (?key=) OR as "x-goog-api-key" header (we use header)
 *  - Body uses "contents" array (not "messages"), each with a "parts" array
 *  - No "role": "system" at top level — put system instruction in first "user" turn or use
 *    the separate "systemInstruction" field (Gemini 1.5+)
 *  - Response text at candidates[0].content.parts[0].text
 *  - Token usage at usageMetadata.promptTokenCount / candidatesTokenCount
 */
public class GeminiClient {

    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final String apiKey;
    private final String model;

    public GeminiClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public LlmResponse chat(String userMessage) throws Exception {
        String url = ENDPOINT_TEMPLATE.formatted(model);

        // Gemini's body structure is different: contents[].parts[]
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", userMessage)))
                )
        );

        // API key as custom header instead of Authorization: Bearer
        String raw = HttpHelper.postWithHeader(url, body, "x-goog-api-key", apiKey);
        JsonNode root = HttpHelper.MAPPER.readTree(raw);

        String content = root.at("/candidates/0/content/parts/0/text").asText();
        int promptTokens = root.at("/usageMetadata/promptTokenCount").asInt();
        int completionTokens = root.at("/usageMetadata/candidatesTokenCount").asInt();

        return new LlmResponse("Google Gemini", model, content, promptTokens, completionTokens);
    }
}
