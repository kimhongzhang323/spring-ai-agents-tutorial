package com.masterclass.llmbasics.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.masterclass.llmbasics.common.HttpHelper;
import com.masterclass.llmbasics.common.LlmResponse;

import java.util.List;
import java.util.Map;

/**
 * Ollama local inference client.
 * Docs: https://github.com/ollama/ollama/blob/main/docs/api.md
 *
 * Key things to notice:
 *  - Runs locally — no API key needed!
 *  - Default base URL: http://localhost:11434
 *  - Ollama also exposes an OpenAI-compatible endpoint at /v1/chat/completions
 *    (we use the native /api/chat here to show the difference)
 *  - Native endpoint uses "stream": false to get a single JSON response
 *  - Models must be pulled first: `ollama pull llama3.2`
 *  - Great for: offline dev, privacy-sensitive data, cost-zero experimentation
 *
 * When Spring AI talks to Ollama, it hits this exact same HTTP API.
 */
public class OllamaClient {

    private final String baseUrl;
    private final String model;

    public OllamaClient(String baseUrl, String model) {
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public OllamaClient(String model) {
        this("http://localhost:11434", model);
    }

    public LlmResponse chat(String userMessage) throws Exception {
        // Native Ollama format (not OpenAI-compat)
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,   // stream:false → single JSON response, not NDJSON stream
                "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                )
        );

        // No bearer token — Ollama runs locally without auth by default
        String raw = HttpHelper.post(baseUrl + "/api/chat", body, null);
        JsonNode root = HttpHelper.MAPPER.readTree(raw);

        String content = root.at("/message/content").asText();
        int promptTokens = root.at("/prompt_eval_count").asInt();
        int completionTokens = root.at("/eval_count").asInt();

        return new LlmResponse("Ollama (local)", model, content, promptTokens, completionTokens);
    }
}
