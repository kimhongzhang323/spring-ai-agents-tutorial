package com.masterclass.llmbasics.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.masterclass.llmbasics.common.HttpHelper;
import com.masterclass.llmbasics.common.LlmResponse;

import java.util.List;
import java.util.Map;

/**
 * Minimal OpenAI Chat Completions client.
 * Docs: https://platform.openai.com/docs/api-reference/chat
 *
 * Key things to notice:
 *  - Endpoint: POST https://api.openai.com/v1/chat/completions
 *  - Auth: "Authorization: Bearer <OPENAI_API_KEY>"
 *  - Body uses "messages" array with role/content pairs
 *  - Response nests the text at choices[0].message.content
 */
public class OpenAiClient {

    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private final String apiKey;
    private final String model;

    public OpenAiClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public LlmResponse chat(String userMessage) throws Exception {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                )
        );

        String raw = HttpHelper.post(ENDPOINT, body, apiKey);
        JsonNode root = HttpHelper.MAPPER.readTree(raw);

        String content = root.at("/choices/0/message/content").asText();
        int promptTokens = root.at("/usage/prompt_tokens").asInt();
        int completionTokens = root.at("/usage/completion_tokens").asInt();

        return new LlmResponse("OpenAI", model, content, promptTokens, completionTokens);
    }
}
