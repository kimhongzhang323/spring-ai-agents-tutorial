package com.masterclass.providers.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Creates named ChatClient beans for each provider.
 *
 * Beans are conditional: if the provider's API key is "not-configured"
 * (the default in application.yml), the bean is not created and the
 * provider is simply unavailable at runtime.
 *
 * This lets you run the module with only Ollama locally and enable
 * cloud providers by exporting their API keys.
 */
@Configuration
public class ProviderConfig {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant. Answer accurately and concisely.
            If you don't know something, say so rather than guessing.
            """;

    // ── OpenAI ──────────────────────────────────────────────────────────────

    @Bean("openaiClient")
    @Primary
    @ConditionalOnProperty(name = "spring.ai.openai.api-key", matchIfMissing = false,
            havingValue = "not-configured", matchIfMissing = false)
    ChatClient openaiClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    // ── Groq (OpenAI-compatible endpoint, different base URL) ───────────────

    @Bean("groqClient")
    @ConditionalOnProperty(name = "GROQ_API_KEY", havingValue = "")
    ChatClient groqClient() {
        // Groq uses the OpenAI wire format at a different base URL
        var groqApi = OpenAiApi.builder()
                .baseUrl("https://api.groq.com/openai")
                .apiKey(System.getenv().getOrDefault("GROQ_API_KEY", "not-configured"))
                .build();
        var options = OpenAiChatOptions.builder()
                .model("llama-3.1-70b-versatile")
                .temperature(0.7)
                .build();
        var model = OpenAiChatModel.builder()
                .openAiApi(groqApi)
                .defaultOptions(options)
                .build();
        return ChatClient.builder(model)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    // ── Anthropic Claude ────────────────────────────────────────────────────

    @Bean("anthropicClient")
    @ConditionalOnProperty(name = "spring.ai.anthropic.api-key", matchIfMissing = false,
            havingValue = "not-configured", matchIfMissing = false)
    ChatClient anthropicClient(AnthropicChatModel anthropicChatModel) {
        return ChatClient.builder(anthropicChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    // ── Google Gemini ────────────────────────────────────────────────────────

    @Bean("geminiClient")
    @ConditionalOnProperty(name = "spring.ai.vertex.ai.gemini.project-id", matchIfMissing = false,
            havingValue = "not-configured", matchIfMissing = false)
    ChatClient geminiClient(VertexAiGeminiChatModel geminiChatModel) {
        return ChatClient.builder(geminiChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    // ── AWS Bedrock ──────────────────────────────────────────────────────────

    @Bean("bedrockClient")
    @ConditionalOnProperty(name = "AWS_REGION")
    ChatClient bedrockClient(BedrockProxyChatModel bedrockChatModel) {
        return ChatClient.builder(bedrockChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    // ── Azure OpenAI ─────────────────────────────────────────────────────────

    @Bean("azureClient")
    @ConditionalOnProperty(name = "spring.ai.azure.openai.api-key", matchIfMissing = false,
            havingValue = "not-configured", matchIfMissing = false)
    ChatClient azureClient(AzureOpenAiChatModel azureChatModel) {
        return ChatClient.builder(azureChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    // ── Mistral AI ────────────────────────────────────────────────────────────

    @Bean("mistralClient")
    @ConditionalOnProperty(name = "spring.ai.mistral.ai.api-key", matchIfMissing = false,
            havingValue = "not-configured", matchIfMissing = false)
    ChatClient mistralClient(MistralAiChatModel mistralChatModel) {
        return ChatClient.builder(mistralChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    // ── Ollama (always available if Docker is running) ────────────────────────

    @Bean("ollamaClient")
    ChatClient ollamaClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
