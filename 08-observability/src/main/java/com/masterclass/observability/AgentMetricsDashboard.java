package com.masterclass.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registers all agent-specific Micrometer metrics.
 *
 * These metrics are scraped by Prometheus (see docker-compose.yml) and visualised
 * in Grafana (import the dashboard JSON from src/main/resources/grafana/).
 *
 * Metric naming follows the OpenTelemetry semantic conventions for generative AI:
 *   https://opentelemetry.io/docs/specs/semconv/gen-ai/
 *
 * Key metrics registered here:
 *   - gen_ai.client.token.usage      (counter, split by token_type=input|output)
 *   - gen_ai.agent.active_sessions   (gauge)
 *   - gen_ai.agent.request.duration  (timer, histogram)
 *   - gen_ai.agent.errors.total      (counter, split by error_type)
 */
@Component
public class AgentMetricsDashboard {

    // Gauge backing — must be held strongly to prevent GC
    private final AtomicInteger activeSessions = new AtomicInteger(0);

    private final Counter inputTokenCounter;
    private final Counter outputTokenCounter;
    private final Counter guardrailRejections;
    private final Timer requestDurationTimer;

    public AgentMetricsDashboard(MeterRegistry registry) {
        this.inputTokenCounter = Counter.builder("gen_ai.client.token.usage")
                .tag("token_type", "input")
                .description("Total input tokens consumed")
                .register(registry);

        this.outputTokenCounter = Counter.builder("gen_ai.client.token.usage")
                .tag("token_type", "output")
                .description("Total output tokens generated")
                .register(registry);

        this.guardrailRejections = Counter.builder("gen_ai.guardrail.rejections.total")
                .description("Requests rejected by input guardrails before reaching the LLM")
                .register(registry);

        this.requestDurationTimer = Timer.builder("gen_ai.agent.request.duration")
                .description("End-to-end agent request duration")
                .publishPercentileHistogram(true)
                .register(registry);

        Gauge.builder("gen_ai.agent.active_sessions", activeSessions, AtomicInteger::get)
                .description("Number of currently active agent sessions")
                .register(registry);
    }

    public void recordTokenUsage(long inputTokens, long outputTokens) {
        inputTokenCounter.increment(inputTokens);
        outputTokenCounter.increment(outputTokens);
    }

    public void recordGuardrailRejection() {
        guardrailRejections.increment();
    }

    public Timer.Sample startRequestTimer() {
        return Timer.start();
    }

    public void stopRequestTimer(Timer.Sample sample) {
        sample.stop(requestDurationTimer);
    }

    public void sessionStarted() {
        activeSessions.incrementAndGet();
    }

    public void sessionEnded() {
        activeSessions.decrementAndGet();
    }
}
