package com.masterclass.providers.router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Routes requests to the appropriate ChatClient based on the routing strategy.
 *
 * Providers that were not configured (missing API key) simply won't have a bean
 * injected — we use Optional<ChatClient> to handle absent providers gracefully.
 *
 * Priority order per strategy (first available wins):
 *   COST:     groq → deepseek → together → mistral → openai → anthropic → ollama
 *   QUALITY:  openai → anthropic → gemini → bedrock → azure → mistral → ollama
 *   BALANCED: anthropic → openai → gemini → mistral → groq → ollama
 *   LOCAL:    ollama only
 *   RESEARCH: perplexity → openai → anthropic → ollama
 */
@Component
public class ProviderRouter {

    private static final Logger log = LoggerFactory.getLogger(ProviderRouter.class);

    private final Map<String, ChatClient> clientsByName;

    public ProviderRouter(
            @Qualifier("ollamaClient") ChatClient ollamaClient,
            @Qualifier("openaiClient") Optional<ChatClient> openaiClient,
            @Qualifier("anthropicClient") Optional<ChatClient> anthropicClient,
            @Qualifier("geminiClient") Optional<ChatClient> geminiClient,
            @Qualifier("groqClient") Optional<ChatClient> groqClient,
            @Qualifier("mistralClient") Optional<ChatClient> mistralClient,
            @Qualifier("azureClient") Optional<ChatClient> azureClient,
            @Qualifier("bedrockClient") Optional<ChatClient> bedrockClient,
            @Qualifier("deepseekClient") Optional<ChatClient> deepseekClient,
            @Qualifier("togetherClient") Optional<ChatClient> togetherClient,
            @Qualifier("perplexityClient") Optional<ChatClient> perplexityClient) {

        var builder = new java.util.HashMap<String, ChatClient>();
        builder.put("ollama", ollamaClient);
        openaiClient.ifPresent(c -> builder.put("openai", c));
        anthropicClient.ifPresent(c -> builder.put("anthropic", c));
        geminiClient.ifPresent(c -> builder.put("gemini", c));
        groqClient.ifPresent(c -> builder.put("groq", c));
        mistralClient.ifPresent(c -> builder.put("mistral", c));
        azureClient.ifPresent(c -> builder.put("azure", c));
        bedrockClient.ifPresent(c -> builder.put("bedrock", c));
        deepseekClient.ifPresent(c -> builder.put("deepseek", c));
        togetherClient.ifPresent(c -> builder.put("together", c));
        perplexityClient.ifPresent(c -> builder.put("perplexity", c));
        this.clientsByName = Map.copyOf(builder);

        log.info("ProviderRouter ready. Available providers: {}", clientsByName.keySet());
    }

    /**
     * Select a ChatClient based on the routing strategy.
     * Falls back to Ollama if no preferred provider is available.
     */
    public ChatClient select(RoutingStrategy strategy, String explicitProvider) {
        List<String> preference = preferenceList(strategy, explicitProvider);
        return preference.stream()
                .filter(clientsByName::containsKey)
                .map(clientsByName::get)
                .findFirst()
                .orElseGet(() -> {
                    log.warn("No preferred provider available for strategy {}; falling back to ollama", strategy);
                    return clientsByName.get("ollama");
                });
    }

    public String selectProviderName(RoutingStrategy strategy, String explicitProvider) {
        List<String> preference = preferenceList(strategy, explicitProvider);
        return preference.stream()
                .filter(clientsByName::containsKey)
                .findFirst()
                .orElse("ollama");
    }

    public java.util.Set<String> availableProviders() {
        return clientsByName.keySet();
    }

    private List<String> preferenceList(RoutingStrategy strategy, String explicit) {
        return switch (strategy) {
            case COST     -> List.of("groq", "deepseek", "together", "mistral", "openai", "anthropic", "gemini", "bedrock", "azure", "ollama");
            case QUALITY  -> List.of("openai", "anthropic", "gemini", "bedrock", "azure", "mistral", "groq", "ollama");
            case BALANCED -> List.of("anthropic", "openai", "gemini", "mistral", "deepseek", "groq", "bedrock", "azure", "ollama");
            case LOCAL    -> List.of("ollama");
            case RESEARCH -> List.of("perplexity", "openai", "anthropic", "ollama");
            case EXPLICIT -> explicit != null ? List.of(explicit.toLowerCase(), "ollama") : List.of("ollama");
        };
    }
}
