package com.masterclass.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterclass.providers.benchmark.BenchmarkReport;
import com.masterclass.providers.benchmark.ProviderBenchmarkService;
import com.masterclass.providers.fallback.FallbackChainService;
import com.masterclass.providers.fallback.FallbackResult;
import com.masterclass.providers.router.ProviderRouter;
import com.masterclass.providers.router.RoutingStrategy;
import com.masterclass.providers.streaming.StreamingService;
import com.masterclass.shared.guardrails.InputValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for the multi-provider API.
 *
 * Key patterns demonstrated:
 * 1. Testing provider selection strategy parsing (COST, QUALITY, BALANCED).
 * 2. Testing fallback response structure (attemptsNeeded, providerUsed).
 * 3. Testing SSE streaming endpoint — verifies Flux wiring with MockMvc.
 * 4. Testing input validation (empty message, too-long message).
 * 5. Verifying that guardrail rejections return 400, not 500.
 */
@WebMvcTest(ProviderController.class)
class ProviderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProviderRouter router;
    @MockBean FallbackChainService fallbackChain;
    @MockBean ProviderBenchmarkService benchmarkService;
    @MockBean StreamingService streamingService;
    @MockBean InputValidator inputValidator;

    @MockBean ChatClient.Builder chatClientBuilder;
    @MockBean ChatClient chatClient;
    @MockBean ChatClient.ChatClientRequestSpec requestSpec;
    @MockBean ChatClient.CallResponseSpec callSpec;

    @BeforeEach
    void setupMocks() {
        when(inputValidator.validate(anyString())).thenReturn(new InputValidator.ValidationResult(true, null));
    }

    @Test
    @WithMockUser
    void availableProvidersReturnsSet() throws Exception {
        when(router.availableProviders()).thenReturn(Set.of("openai", "anthropic", "ollama"));

        mockMvc.perform(get("/api/v1/providers/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void availableProvidersRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/providers/available"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void chatReturnsBothResponseAndProviderName() throws Exception {
        when(router.selectProviderName(any(), any())).thenReturn("openai");
        when(router.select(any(), any())).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("AI is transforming software development.");

        mockMvc.perform(post("/api/v1/providers/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProviderController.ChatRequest("What is AI?", "BALANCED", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("AI is transforming software development."))
                .andExpect(jsonPath("$.providerUsed").value("openai"))
                .andExpect(jsonPath("$.strategyApplied").value("BALANCED"));
    }

    @Test
    @WithMockUser
    void chatDefaultsToBalancedStrategyWhenNotSpecified() throws Exception {
        when(router.selectProviderName(eq(RoutingStrategy.BALANCED), isNull())).thenReturn("anthropic");
        when(router.select(eq(RoutingStrategy.BALANCED), isNull())).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Answer");

        mockMvc.perform(post("/api/v1/providers/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProviderController.ChatRequest("Hello", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategyApplied").value("BALANCED"));
    }

    @Test
    @WithMockUser
    void chatWithGuardrailRejectionReturnsBadRequest() throws Exception {
        when(inputValidator.validate(anyString()))
                .thenReturn(new InputValidator.ValidationResult(false, "Message too long"));

        mockMvc.perform(post("/api/v1/providers/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProviderController.ChatRequest("x".repeat(5000), null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void fallbackEndpointReturnsFallbackMetadata() throws Exception {
        when(fallbackChain.execute(anyString(), any()))
                .thenReturn(new FallbackResult("Fallback answer", "ollama", 3, null));

        mockMvc.perform(post("/api/v1/providers/chat/fallback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProviderController.ChatRequest("Explain RAG", "COST", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Fallback answer"))
                .andExpect(jsonPath("$.providerUsed").value("ollama"))
                .andExpect(jsonPath("$.attemptsNeeded").value(3))
                .andExpect(jsonPath("$.succeeded").value(true));
    }

    @Test
    @WithMockUser
    void streamEndpointReturnsEventStream() throws Exception {
        when(streamingService.stream(anyString(), any(), any()))
                .thenReturn(Flux.just("Hello", " World", "!"));

        mockMvc.perform(post("/api/v1/providers/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE)
                        .content(objectMapper.writeValueAsString(
                                new ProviderController.ChatRequest("Say hello", null, null))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    @WithMockUser
    void benchmarkEndpointReturnsBenchmarkReport() throws Exception {
        var report = new BenchmarkReport(
                "What is Java?",
                List.of(new BenchmarkReport.ProviderResult("openai", "Java is...", 850L, "SUCCESS", null, 0.002)),
                "openai",
                "openai"
        );
        when(benchmarkService.benchmark(anyString(), anyList())).thenReturn(report);

        mockMvc.perform(post("/api/v1/providers/benchmark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProviderController.BenchmarkRequest("What is Java?",
                                        List.of("openai")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fastestProvider").value("openai"))
                .andExpect(jsonPath("$.results[0].provider").value("openai"));
    }

    @Test
    @WithMockUser
    void emptyMessageIsRejectedWithValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/providers/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProviderController.ChatRequest("", null, null))))
                .andExpect(status().isBadRequest());
    }
}
