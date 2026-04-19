package com.masterclass.llmbasics.qwen;

import com.fasterxml.jackson.databind.JsonNode;
import com.masterclass.llmbasics.common.HttpHelper;
import com.masterclass.llmbasics.common.LlmResponse;

import java.util.List;
import java.util.Map;

/**
 * Alibaba Cloud Qwen (通义千问) client via DashScope API.
 * Docs: https://www.alibabacloud.com/help/en/model-studio/developer-reference/use-qwen-by-calling-api
 *
 * Key things to notice:
 *  - DashScope exposes an OpenAI-compatible endpoint — same JSON structure as OpenAI!
 *  - Endpoint: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
 *  - Auth: "Authorization: Bearer <DASHSCOPE_API_KEY>"
 *  - Models: qwen-turbo, qwen-plus, qwen-max, qwen-long
 *  - Because it's OpenAI-compatible, switching from OpenAI to Qwen only requires
 *    changing the base URL and API key — the body stays identical.
 *
 * This is exactly how Spring AI's multi-provider support works under the hood.
 */
public class QwenClient {

    // OpenAI-compatible endpoint — note the path matches OpenAI's exactly
    private static final String ENDPOINT =
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    private final String apiKey;
    private final String model;

    public QwenClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public LlmResponse chat(String userMessage) throws Exception {
        // Body is 100% identical to OpenAI — this is the power of the OpenAI-compat standard
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

        return new LlmResponse("Qwen (Alibaba)", model, content, promptTokens, completionTokens);
    }
}
