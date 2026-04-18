package com.masterclass.providers.fallback;

import com.masterclass.providers.router.ProviderRouter;
import com.masterclass.providers.router.RoutingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Tries providers in priority order and returns the first successful response.
 *
 * This implements the "fallback chain" pattern: if GPT-4o is down, try Claude;
 * if Claude is also unavailable, try Groq; if everything fails, return a
 * graceful degraded response.
 *
 * Each attempt is logged so you can see in Grafana how often fallbacks trigger.
 */
@Service
public class FallbackChainService {

    private static final Logger log = LoggerFactory.getLogger(FallbackChainService.class);

    private final ProviderRouter router;

    public FallbackChainService(ProviderRouter router) {
        this.router = router;
    }

    /**
     * Attempts the prompt against providers in priority order.
     * Returns as soon as one succeeds.
     *
     * @param prompt   the user prompt
     * @param strategy routing strategy (determines priority order)
     * @return FallbackResult with the response, provider used, and number of attempts
     */
    public FallbackResult execute(String prompt, RoutingStrategy strategy) {
        List<String> candidates = new java.util.ArrayList<>(router.availableProviders());
        sortByStrategy(candidates, strategy);

        int attempt = 0;
        for (String providerName : candidates) {
            attempt++;
            try {
                log.debug("FallbackChain attempt {} — trying provider: {}", attempt, providerName);
                String response = router.select(RoutingStrategy.EXPLICIT, providerName)
                        .prompt()
                        .user(prompt)
                        .call()
                        .content();
                log.info("FallbackChain succeeded on attempt {} with provider: {}", attempt, providerName);
                return new FallbackResult(response, providerName, attempt, null);
            } catch (Exception e) {
                log.warn("FallbackChain attempt {} failed for provider {}: {}", attempt, providerName, e.getMessage());
            }
        }

        log.error("FallbackChain exhausted all {} providers", candidates.size());
        return new FallbackResult(
                "I'm experiencing technical difficulties. All AI providers are currently unavailable. Please try again later.",
                "none",
                attempt,
                "All providers exhausted"
        );
    }

    private void sortByStrategy(List<String> providers, RoutingStrategy strategy) {
        List<String> preferenceOrder = switch (strategy) {
            case COST     -> List.of("groq", "mistral", "openai", "anthropic", "gemini", "ollama");
            case QUALITY  -> List.of("openai", "anthropic", "gemini", "mistral", "groq", "ollama");
            case BALANCED -> List.of("anthropic", "openai", "gemini", "mistral", "groq", "ollama");
            case LOCAL    -> List.of("ollama");
            case EXPLICIT -> providers; // caller handles ordering
        };
        providers.sort((a, b) -> {
            int ai = preferenceOrder.indexOf(a);
            int bi = preferenceOrder.indexOf(b);
            if (ai == -1) ai = 999;
            if (bi == -1) bi = 999;
            return Integer.compare(ai, bi);
        });
    }
}
