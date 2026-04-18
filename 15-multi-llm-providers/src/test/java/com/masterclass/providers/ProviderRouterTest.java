package com.masterclass.providers;

import com.masterclass.providers.router.ProviderRouter;
import com.masterclass.providers.router.RoutingStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProviderRouterTest {

    private final ChatClient ollamaClient = mock(ChatClient.class);
    private final ChatClient openaiClient = mock(ChatClient.class);
    private final ChatClient anthropicClient = mock(ChatClient.class);

    private ProviderRouter routerWithAll() {
        return new ProviderRouter(
                ollamaClient,
                Optional.of(openaiClient),
                Optional.of(anthropicClient),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    private ProviderRouter routerOllamaOnly() {
        return new ProviderRouter(
                ollamaClient,
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
        );
    }

    @Test
    void qualityStrategyPrefersOpenAI() {
        var router = routerWithAll();
        String name = router.selectProviderName(RoutingStrategy.QUALITY, null);
        assertThat(name).isEqualTo("openai");
    }

    @Test
    void balancedStrategyPrefersAnthropic() {
        var router = routerWithAll();
        String name = router.selectProviderName(RoutingStrategy.BALANCED, null);
        assertThat(name).isEqualTo("anthropic");
    }

    @Test
    void localStrategyAlwaysPicksOllama() {
        var router = routerWithAll();
        String name = router.selectProviderName(RoutingStrategy.LOCAL, null);
        assertThat(name).isEqualTo("ollama");
    }

    @Test
    void explicitStrategyPicksNamedProvider() {
        var router = routerWithAll();
        String name = router.selectProviderName(RoutingStrategy.EXPLICIT, "anthropic");
        assertThat(name).isEqualTo("anthropic");
    }

    @Test
    void fallsBackToOllamaWhenPreferredUnavailable() {
        var router = routerOllamaOnly();
        String name = router.selectProviderName(RoutingStrategy.QUALITY, null);
        assertThat(name).isEqualTo("ollama");
    }

    @Test
    void availableProvidersReflectsConfiguration() {
        var router = routerWithAll();
        assertThat(router.availableProviders()).containsExactlyInAnyOrder("ollama", "openai", "anthropic");
    }
}
