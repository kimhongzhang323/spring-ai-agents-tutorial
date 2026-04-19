package com.masterclass.providers;

import com.masterclass.providers.fallback.FallbackChainService;
import com.masterclass.providers.router.ProviderRouter;
import com.masterclass.providers.router.RoutingStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FallbackChainServiceTest {

    @Test
    void returnsFirstSuccessfulProviderResponse() {
        var ollamaClient = mockChatClientReturning("ollama response");

        var router = new ProviderRouter(
                ollamaClient,
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
        );
        var service = new FallbackChainService(router);

        var result = service.execute("hello", RoutingStrategy.LOCAL);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.response()).isEqualTo("ollama response");
        assertThat(result.providerUsed()).isEqualTo("ollama");
        assertThat(result.attemptsNeeded()).isEqualTo(1);
    }

    @Test
    void fallsBackToSecondProviderWhenFirstFails() {
        var failingClient = mockChatClientThrowing(new RuntimeException("provider down"));
        var successClient = mockChatClientReturning("fallback response");

        // openai is configured but fails; ollama succeeds
        var router = new ProviderRouter(
                successClient,             // ollama
                Optional.of(failingClient), // openai
                Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
        );
        var service = new FallbackChainService(router);

        var result = service.execute("hello", RoutingStrategy.QUALITY);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.attemptsNeeded()).isGreaterThan(1);
    }

    @Test
    void returnsGracefulDegradationWhenAllProvidersFail() {
        var failingClient = mockChatClientThrowing(new RuntimeException("all down"));

        var router = new ProviderRouter(
                failingClient,
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
        );
        var service = new FallbackChainService(router);

        var result = service.execute("hello", RoutingStrategy.LOCAL);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.response()).contains("unavailable");
        assertThat(result.errorMessage()).isNotNull();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private ChatClient mockChatClientReturning(String response) {
        var callSpec = mock(ChatClient.CallResponseSpec.class);
        when(callSpec.content()).thenReturn(response);

        var promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        when(promptSpec.user(any(String.class))).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);

        var client = mock(ChatClient.class);
        when(client.prompt()).thenReturn(promptSpec);
        return client;
    }

    @SuppressWarnings("unchecked")
    private ChatClient mockChatClientThrowing(RuntimeException ex) {
        var callSpec = mock(ChatClient.CallResponseSpec.class);
        when(callSpec.content()).thenThrow(ex);

        var promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        when(promptSpec.user(any(String.class))).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);

        var client = mock(ChatClient.class);
        when(client.prompt()).thenReturn(promptSpec);
        return client;
    }
}
