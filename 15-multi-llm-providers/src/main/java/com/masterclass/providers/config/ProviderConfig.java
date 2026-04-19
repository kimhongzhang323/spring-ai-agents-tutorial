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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates a named ChatClient bean for each LLM provider.
 *
 * Strategy: rely on Spring AI's own auto-configuration to create ChatModel beans.
 * Each Spring AI starter creates its ChatModel bean only when the required
 * property (api-key, endpoint, etc.) is present and non-default.
 * We then wrap each ChatModel in a named ChatClient bean using @ConditionalOnBean.
 *
 * This means:
 *   - Export OPENAI_API_KEY  → "openaiClient" bean appears
 *   - Export ANTHROPIC_API_KEY → "anthropicClient" bean appears
 *   - Ollama always available (no key needed)
 *   - ProviderRouter receives Optional<ChatClient> for each — absent = not configured
 */
@Configuration
public class ProviderConfig {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant. Answer accurately and concisely.
            If you don't know something, say so rather than guessing.
            """;

    // ── OpenAI ───────────────────────────────────────────────────────────────
    // Spring AI creates OpenAiChatModel bean only when spring.ai.openai.api-key != "not-configured"

    @Bean("openaiClient")
    @ConditionalOnBean(OpenAiChatModel.class)
    ChatClient openaiClient(OpenAiChatModel model) {
        return ChatClient.builder(model).defaultSystem(SYSTEM_PROMPT).build();
    }

    // ── Groq (OpenAI-compatible, different base URL) ──────────────────────────
    // Groq has no Spring AI starter — we manually build the OpenAiChatModel with Groq's URL

    @Bean("groqClient")
    @ConditionalOnProperty(name = "GROQ_API_KEY")
    ChatClient groqClient() {
        var api = OpenAiApi.builder()
                .baseUrl("https://api.groq.com/openai")
                .apiKey(System.getenv("GROQ_API_KEY"))
                .build();
        var model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("llama-3.1-70b-versatile")
                        .temperature(0.7)
                        .build())
                .build();
        return ChatClient.builder(model).defaultSystem(SYSTEM_PROMPT).build();
    }

    // ── Anthropic Claude ──────────────────────────────────────────────────────

    @Bean("anthropicClient")
    @ConditionalOnBean(AnthropicChatModel.class)
    ChatClient anthropicClient(AnthropicChatModel model) {
        return ChatClient.builder(model).defaultSystem(SYSTEM_PROMPT).build();
    }

    // ── Google Gemini ─────────────────────────────────────────────────────────

    @Bean("geminiClient")
    @ConditionalOnBean(VertexAiGeminiChatModel.class)
    ChatClient geminiClient(VertexAiGeminiChatModel model) {
        return ChatClient.builder(model).defaultSystem(SYSTEM_PROMPT).build();
    }

    // ── AWS Bedrock ───────────────────────────────────────────────────────────

    @Bean("bedrockClient")
    @ConditionalOnBean(BedrockProxyChatModel.class)
    ChatClient bedrockClient(BedrockProxyChatModel model) {
        return ChatClient.builder(model).defaultSystem(SYSTEM_PROMPT).build();
    }

    // ── Azure OpenAI ──────────────────────────────────────────────────────────

    @Bean("azureClient")
    @ConditionalOnBean(AzureOpenAiChatModel.class)
    ChatClient azureClient(AzureOpenAiChatModel model) {
        return ChatClient.builder(model).defaultSystem(SYSTEM_PROMPT).build();
    }

    // ── Mistral AI ────────────────────────────────────────────────────────────

    @Bean("mistralClient")
    @ConditionalOnBean(MistralAiChatModel.class)
    ChatClient mistralClient(MistralAiChatModel model) {
        return ChatClient.builder(model).defaultSystem(SYSTEM_PROMPT).build();
    }

    // ── DeepSeek — OpenAI-compatible API ──────────────────────────────────────
    // DeepSeek offers strong reasoning at low cost; uses the OpenAI-compatible endpoint.
    // Set DEEPSEEK_API_KEY to enable.

    @Bean("deepseekClient")
    @ConditionalOnProperty(name = "DEEPSEEK_API_KEY", matchIfMissing = false)
    ChatClient deepseekClient() {
        var api = OpenAiApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .build();
        var model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("deepseek-chat")
                        .temperature(0.7)
                        .build())
                .build();
        return ChatClient.builder(model).defaultSystem(SYSTEM_PROMPT).build();
    }

    // ── Together AI — OpenAI-compatible, hosts 100+ open models ──────────────
    // Good for running Llama, Mistral, Qwen, etc. in the cloud.
    // Set TOGETHER_API_KEY to enable.

    @Bean("togetherClient")
    @ConditionalOnProperty(name = "TOGETHER_API_KEY", matchIfMissing = false)
    ChatClient togetherClient() {
        var api = OpenAiApi.builder()
                .baseUrl("https://api.together.xyz/v1")
                .apiKey(System.getenv("TOGETHER_API_KEY"))
                .build();
        var model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo")
                        .temperature(0.7)
                        .build())
                .build();
        return ChatClient.builder(model).defaultSystem(SYSTEM_PROMPT).build();
    }

    // ── Perplexity AI — OpenAI-compatible, with web search grounding ─────────
    // Perplexity models can access current web data — great for factual queries.
    // Set PERPLEXITY_API_KEY to enable.

    @Bean("perplexityClient")
    @ConditionalOnProperty(name = "PERPLEXITY_API_KEY", matchIfMissing = false)
    ChatClient perplexityClient() {
        var api = OpenAiApi.builder()
                .baseUrl("https://api.perplexity.ai")
                .apiKey(System.getenv("PERPLEXITY_API_KEY"))
                .build();
        var model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("sonar-pro")
                        .temperature(0.7)
                        .build())
                .build();
        return ChatClient.builder(model).defaultSystem(SYSTEM_PROMPT).build();
    }

    // ── Ollama — always present; this is the last-resort fallback ─────────────

    @Bean("ollamaClient")
    ChatClient ollamaClient(OllamaChatModel model) {
        return ChatClient.builder(model).defaultSystem(SYSTEM_PROMPT).build();
    }
}
