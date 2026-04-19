package com.masterclass.providers;

import com.masterclass.providers.router.ProviderRouter;
import com.masterclass.providers.router.RoutingStrategy;
import com.masterclass.providers.tuning.ResponseConfig;
import com.masterclass.providers.tuning.ResponseTuningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponseTuningServiceTest {

    @Mock
    ProviderRouter router;

    @Mock
    ChatClient chatClient;

    @Mock
    ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    ChatClient.CallResponseSpec callSpec;

    @InjectMocks
    ResponseTuningService service;

    @Test
    void chat_returnsResponseWithProviderAndConfig() {
        when(router.select(any(), any())).thenReturn(chatClient);
        when(router.selectProviderName(any(), any())).thenReturn("openai");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Hello from OpenAI!");

        var config = new ResponseConfig(0.5, 256, null, null, List.of(), null);
        var result = service.chat("Say hello", "openai", config);

        assertThat(result.response()).isEqualTo("Hello from OpenAI!");
        assertThat(result.providerUsed()).isEqualTo("openai");
        assertThat(result.configApplied().temperature()).isEqualTo(0.5);
        assertThat(result.configApplied().maxTokens()).isEqualTo(256);
        assertThat(result.latencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void factualPreset_usesLowTemperature() {
        assertThat(ResponseConfig.factual().temperature()).isEqualTo(0.1);
        assertThat(ResponseConfig.factual().maxTokens()).isEqualTo(512);
    }

    @Test
    void creativePreset_usesHighTemperature() {
        assertThat(ResponseConfig.creative().temperature()).isEqualTo(1.0);
        assertThat(ResponseConfig.creative().maxTokens()).isEqualTo(2048);
    }

    @Test
    void chatWithStop_passesStopSequenceInConfig() {
        when(router.select(any(), any())).thenReturn(chatClient);
        when(router.selectProviderName(any(), any())).thenReturn("ollama");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Step 1. Do this.");

        var result = service.chatWithStop("List the steps", "###", null);

        assertThat(result.response()).isEqualTo("Step 1. Do this.");
        assertThat(result.configApplied().stopSequences()).contains("###");
    }

    @Test
    void noExplicitProvider_usesBalancedStrategy() {
        when(router.select(eq(RoutingStrategy.BALANCED), isNull())).thenReturn(chatClient);
        when(router.selectProviderName(eq(RoutingStrategy.BALANCED), isNull())).thenReturn("anthropic");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("response");

        var result = service.chat("test", null, ResponseConfig.defaults());

        assertThat(result.providerUsed()).isEqualTo("anthropic");
    }
}
