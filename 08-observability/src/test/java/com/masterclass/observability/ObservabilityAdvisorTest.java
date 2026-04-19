package com.masterclass.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests for ObservabilityAdvisor — verifying that metrics are emitted correctly
 * for success and error paths.
 *
 * We use {@link SimpleMeterRegistry} (no Prometheus server needed) to capture
 * counter increments in-memory and assert on them. This is the canonical way to
 * unit-test Micrometer instrumentation.
 *
 * The ObservationRegistry is configured in no-op mode for tests — we're not
 * testing span creation here (that's an integration test concern), only metrics.
 */
class ObservabilityAdvisorTest {

    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

    ObservabilityAdvisor advisor;

    @BeforeEach
    void setup() {
        advisor = new ObservabilityAdvisor(observationRegistry, meterRegistry);
    }

    @Test
    void incrementsCallCounterOnSuccessfulCall() {
        var chain = mock(CallAdvisorChain.class);
        var response = mockSuccessResponse();
        when(chain.nextCall(any())).thenReturn(response);

        advisor.adviseCall(mock(ChatClientRequest.class), chain);

        Counter callCounter = meterRegistry.find("llm.calls.total").counter();
        assertThat(callCounter).isNotNull();
        assertThat(callCounter.count()).isEqualTo(1.0);
    }

    @Test
    void callCounterAccumulatesAcrossMultipleCalls() {
        var chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenReturn(mockSuccessResponse());

        advisor.adviseCall(mock(ChatClientRequest.class), chain);
        advisor.adviseCall(mock(ChatClientRequest.class), chain);
        advisor.adviseCall(mock(ChatClientRequest.class), chain);

        Counter callCounter = meterRegistry.find("llm.calls.total").counter();
        assertThat(callCounter.count()).isEqualTo(3.0);
    }

    @Test
    void incrementsErrorCounterWhenChainThrows() {
        var chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenThrow(new RuntimeException("LLM timeout"));

        assertThatThrownBy(() -> advisor.adviseCall(mock(ChatClientRequest.class), chain))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("LLM timeout");

        Counter errorCounter = meterRegistry.find("llm.errors.total").counter();
        assertThat(errorCounter).isNotNull();
        assertThat(errorCounter.count()).isEqualTo(1.0);
    }

    @Test
    void doesNotIncrementErrorCounterOnSuccess() {
        var chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenReturn(mockSuccessResponse());

        advisor.adviseCall(mock(ChatClientRequest.class), chain);

        Counter errorCounter = meterRegistry.find("llm.errors.total").counter();
        assertThat(errorCounter.count()).isZero();
    }

    @Test
    void errorCounterIncrementedOnlyForFailures() {
        var chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any()))
                .thenReturn(mockSuccessResponse())   // call 1: success
                .thenThrow(new RuntimeException("fail")) // call 2: fail
                .thenReturn(mockSuccessResponse());  // call 3: success

        advisor.adviseCall(mock(ChatClientRequest.class), chain);
        try { advisor.adviseCall(mock(ChatClientRequest.class), chain); } catch (Exception ignored) {}
        advisor.adviseCall(mock(ChatClientRequest.class), chain);

        assertThat(meterRegistry.find("llm.calls.total").counter().count()).isEqualTo(3.0);
        assertThat(meterRegistry.find("llm.errors.total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void advisorOrderIsLowest() {
        assertThat(advisor.getOrder()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void advisorNameIsCorrect() {
        assertThat(advisor.getName()).isEqualTo("ObservabilityAdvisor");
    }

    @Test
    void exceptionIsRethrowedAfterMetricRecording() {
        var chain = mock(CallAdvisorChain.class);
        var cause = new IllegalStateException("provider quota exceeded");
        when(chain.nextCall(any())).thenThrow(cause);

        assertThatThrownBy(() -> advisor.adviseCall(mock(ChatClientRequest.class), chain))
                .isSameAs(cause);
    }

    private ChatClientResponse mockSuccessResponse() {
        var chatResponse = mock(ChatResponse.class);
        var metadata = ChatResponseMetadata.builder()
                .usage(new DefaultUsage(100L, 50L))
                .build();
        when(chatResponse.getMetadata()).thenReturn(metadata);
        return new ChatClientResponse(chatResponse, java.util.Map.of());
    }
}
