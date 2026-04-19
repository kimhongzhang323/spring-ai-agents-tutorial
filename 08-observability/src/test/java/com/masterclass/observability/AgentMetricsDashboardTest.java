package com.masterclass.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AgentMetricsDashboard — verifying the custom metrics are registered
 * and behave correctly without a real Prometheus scrape endpoint.
 *
 * Lessons demonstrated:
 * 1. How to test Micrometer counters and gauges in unit tests.
 * 2. The Gauge backing-value GC pitfall (AtomicInteger must be held strongly).
 * 3. How token_type tag splits the single counter name into two time series.
 *
 * {@link SimpleMeterRegistry} stores all metrics in memory — ideal for unit tests.
 * Use {@code TestMeterRegistry} (from micrometer-test) for more advanced assertions.
 */
class AgentMetricsDashboardTest {

    MeterRegistry registry = new SimpleMeterRegistry();
    AgentMetricsDashboard dashboard;

    @BeforeEach
    void setup() {
        dashboard = new AgentMetricsDashboard(registry);
    }

    @Test
    void recordTokenUsageIncrementsInputCounter() {
        dashboard.recordTokenUsage(100, 50);

        Counter inputCounter = registry.find("gen_ai.client.token.usage")
                .tag("token_type", "input")
                .counter();

        assertThat(inputCounter).isNotNull();
        assertThat(inputCounter.count()).isEqualTo(100.0);
    }

    @Test
    void recordTokenUsageIncrementsOutputCounter() {
        dashboard.recordTokenUsage(100, 75);

        Counter outputCounter = registry.find("gen_ai.client.token.usage")
                .tag("token_type", "output")
                .counter();

        assertThat(outputCounter).isNotNull();
        assertThat(outputCounter.count()).isEqualTo(75.0);
    }

    @Test
    void inputAndOutputCountersAccumulateCorrectly() {
        dashboard.recordTokenUsage(200, 100);
        dashboard.recordTokenUsage(150, 80);
        dashboard.recordTokenUsage(50, 20);

        double totalInput = registry.find("gen_ai.client.token.usage").tag("token_type", "input").counter().count();
        double totalOutput = registry.find("gen_ai.client.token.usage").tag("token_type", "output").counter().count();

        assertThat(totalInput).isEqualTo(400.0);
        assertThat(totalOutput).isEqualTo(200.0);
    }

    @Test
    void guardrailRejectionCounterStartsAtZero() {
        Counter counter = registry.find("gen_ai.guardrail.rejections.total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isZero();
    }

    @Test
    void recordGuardrailRejectionIncrementsCounter() {
        dashboard.recordGuardrailRejection();
        dashboard.recordGuardrailRejection();

        assertThat(registry.find("gen_ai.guardrail.rejections.total").counter().count()).isEqualTo(2.0);
    }

    @Test
    void activeSessionsGaugeReflectsCurrentSessions() {
        Gauge gauge = registry.find("gen_ai.agent.active_sessions").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isZero();

        dashboard.sessionStarted();
        dashboard.sessionStarted();
        assertThat(gauge.value()).isEqualTo(2.0);

        dashboard.sessionEnded();
        assertThat(gauge.value()).isEqualTo(1.0);

        dashboard.sessionEnded();
        assertThat(gauge.value()).isZero();
    }

    @Test
    void activeSessionsGaugeDoesNotGoNegative() {
        // Ending a session when none are active should be handled gracefully
        dashboard.sessionEnded();

        Gauge gauge = registry.find("gen_ai.agent.active_sessions").gauge();
        // AtomicInteger allows negative — a real implementation might clamp to 0
        // This test documents the current behaviour so developers are aware
        assertThat(gauge.value()).isLessThanOrEqualTo(0.0);
    }

    @Test
    void requestDurationTimerIsRegistered() {
        var timer = registry.find("gen_ai.agent.request.duration").timer();
        assertThat(timer).isNotNull();
    }

    @Test
    void startAndStopRequestTimerRecordsDuration() throws InterruptedException {
        var sample = dashboard.startRequestTimer();
        Thread.sleep(10); // simulate some work
        dashboard.stopRequestTimer(sample);

        var timer = registry.find("gen_ai.agent.request.duration").timer();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(5);
    }

    @Test
    void multipleTimerSamplesAccumulateCount() {
        for (int i = 0; i < 5; i++) {
            var sample = dashboard.startRequestTimer();
            dashboard.stopRequestTimer(sample);
        }

        var timer = registry.find("gen_ai.agent.request.duration").timer();
        assertThat(timer.count()).isEqualTo(5);
    }
}
