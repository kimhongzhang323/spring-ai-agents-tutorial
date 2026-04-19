package com.masterclass.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests for TokenBudgetAdvisor — verifying that over-budget requests are
 * rejected before reaching the LLM and that metrics are recorded correctly.
 *
 * These tests demonstrate how to verify BLOCKING behaviour in a Spring AI advisor:
 * if the advisor throws, the LLM chain should NEVER be called (verified via
 * {@code verify(chain, never()).nextCall(any())}).
 */
class TokenBudgetAdvisorTest {

    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    TokenBudgetAdvisor advisor;

    @BeforeEach
    void setup() {
        advisor = new TokenBudgetAdvisor(registry);
    }

    @Test
    void shortMessagePassesThroughToChain() {
        var chain = mock(CallAdvisorChain.class);
        var response = mockResponse();
        when(chain.nextCall(any())).thenReturn(response);

        ChatClientRequest request = requestWithUserText("Hello, how are you?");
        ChatClientResponse result = advisor.adviseCall(request, chain);

        assertThat(result).isNotNull();
        verify(chain).nextCall(any());
    }

    @Test
    void massiveInputIsRejectedBeforeLlmCall() {
        var chain = mock(CallAdvisorChain.class);
        // 8001 tokens × 4 chars = 32,004 chars — exceeds 8000 token budget
        String hugeInput = "a".repeat(32_004 * 4);

        ChatClientRequest request = requestWithUserText(hugeInput);

        assertThatThrownBy(() -> advisor.adviseCall(request, chain))
                .isInstanceOf(TokenBudgetAdvisor.TokenBudgetExceededException.class)
                .hasMessageContaining("input token budget");

        // The chain must NOT be called when the budget is exceeded
        verify(chain, never()).nextCall(any());
    }

    @Test
    void inputBudgetExceededMetricIsIncrementedOnRejection() {
        var chain = mock(CallAdvisorChain.class);
        String hugeInput = "x".repeat(200_000); // definitely over budget

        try {
            advisor.adviseCall(requestWithUserText(hugeInput), chain);
        } catch (TokenBudgetAdvisor.TokenBudgetExceededException ignored) {}

        double count = registry.find("llm.budget.exceeded")
                .tag("direction", "input")
                .counter()
                .count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void inputBudgetMetricNotIncrementedForNormalRequests() {
        var chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenReturn(mockResponse());

        advisor.adviseCall(requestWithUserText("Normal message"), chain);

        double count = registry.find("llm.budget.exceeded")
                .tag("direction", "input")
                .counter()
                .count();
        assertThat(count).isZero();
    }

    @Test
    void advisorRunsBeforeObservabilityAdvisor() {
        var observabilityAdvisor = new ObservabilityAdvisor(
                io.micrometer.observation.ObservationRegistry.NOOP, registry);

        assertThat(advisor.getOrder()).isGreaterThan(observabilityAdvisor.getOrder());
    }

    @Test
    void largeSystemTextIsIncludedInTokenEstimate() {
        var chain = mock(CallAdvisorChain.class);
        // Build a Prompt with a very large system message
        String largeText = "s".repeat(200_000);
        var prompt = new Prompt(new org.springframework.ai.chat.messages.SystemMessage(largeText));
        ChatClientRequest request = new ChatClientRequest(prompt, java.util.Map.of());

        assertThatThrownBy(() -> advisor.adviseCall(request, chain))
                .isInstanceOf(TokenBudgetAdvisor.TokenBudgetExceededException.class);

        verify(chain, never()).nextCall(any());
    }

    private ChatClientRequest requestWithUserText(String text) {
        return new ChatClientRequest(new Prompt(text), java.util.Map.of());
    }

    private ChatClientResponse mockResponse() {
        return new ChatClientResponse(mock(ChatResponse.class), java.util.Map.of());
    }
}
