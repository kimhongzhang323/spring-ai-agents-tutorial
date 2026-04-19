package com.masterclass.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;

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
 * {@code verify(chain, never()).nextAroundCall(any())}).
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
        var chain = mock(CallAroundAdvisorChain.class);
        var response = mockResponse();
        when(chain.nextAroundCall(any())).thenReturn(response);

        AdvisedRequest request = requestWithUserText("Hello, how are you?");
        AdvisedResponse result = advisor.aroundCall(request, chain);

        assertThat(result).isNotNull();
        verify(chain).nextAroundCall(any());
    }

    @Test
    void massiveInputIsRejectedBeforeLlmCall() {
        var chain = mock(CallAroundAdvisorChain.class);
        // 8001 tokens × 4 chars = 32,004 chars — exceeds 8000 token budget
        String hugeInput = "a".repeat(32_004 * 4);

        AdvisedRequest request = requestWithUserText(hugeInput);

        assertThatThrownBy(() -> advisor.aroundCall(request, chain))
                .isInstanceOf(TokenBudgetAdvisor.TokenBudgetExceededException.class)
                .hasMessageContaining("input token budget");

        // The chain must NOT be called when the budget is exceeded
        verify(chain, never()).nextAroundCall(any());
    }

    @Test
    void inputBudgetExceededMetricIsIncrementedOnRejection() {
        var chain = mock(CallAroundAdvisorChain.class);
        String hugeInput = "x".repeat(200_000); // definitely over budget

        try {
            advisor.aroundCall(requestWithUserText(hugeInput), chain);
        } catch (TokenBudgetAdvisor.TokenBudgetExceededException ignored) {}

        double count = registry.find("llm.budget.exceeded")
                .tag("direction", "input")
                .counter()
                .count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void inputBudgetMetricNotIncrementedForNormalRequests() {
        var chain = mock(CallAroundAdvisorChain.class);
        when(chain.nextAroundCall(any())).thenReturn(mockResponse());

        advisor.aroundCall(requestWithUserText("Normal message"), chain);

        double count = registry.find("llm.budget.exceeded")
                .tag("direction", "input")
                .counter()
                .count();
        assertThat(count).isZero();
    }

    @Test
    void advisorRunsBeforeObservabilityAdvisor() {
        // TokenBudgetAdvisor must run before ObservabilityAdvisor (MIN_VALUE)
        // so rejected requests don't generate misleading success spans
        var observabilityAdvisor = new ObservabilityAdvisor(
                io.micrometer.observation.ObservationRegistry.NOOP, registry);

        assertThat(advisor.getOrder()).isGreaterThan(observabilityAdvisor.getOrder());
    }

    @Test
    void systemTextIsIncludedInTokenEstimate() {
        var chain = mock(CallAroundAdvisorChain.class);
        // Build a request with large system text that pushes over the budget
        String largeSystemText = "s".repeat(200_000);

        AdvisedRequest request = AdvisedRequest.builder()
                .systemText(largeSystemText)
                .userText("short user message")
                .messages(List.of())
                .advisors(List.of())
                .advisorParams(java.util.Map.of())
                .build();

        assertThatThrownBy(() -> advisor.aroundCall(request, chain))
                .isInstanceOf(TokenBudgetAdvisor.TokenBudgetExceededException.class);

        verify(chain, never()).nextAroundCall(any());
    }

    private AdvisedRequest requestWithUserText(String text) {
        return AdvisedRequest.builder()
                .userText(text)
                .messages(List.of())
                .advisors(List.of())
                .advisorParams(java.util.Map.of())
                .build();
    }

    private AdvisedResponse mockResponse() {
        return new AdvisedResponse(mock(ChatResponse.class), java.util.Map.of());
    }
}
