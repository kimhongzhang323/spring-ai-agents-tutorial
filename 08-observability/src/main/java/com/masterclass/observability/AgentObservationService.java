package com.masterclass.observability;

import com.masterclass.shared.guardrails.InputValidator;
import com.masterclass.shared.observability.TokenUsageMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Service;

@Service
public class AgentObservationService {

    private static final Logger log = LoggerFactory.getLogger(AgentObservationService.class);

    private final ChatClient chatClient;
    private final ObservationRegistry observationRegistry;
    private final TokenUsageMetrics tokenUsageMetrics;
    private final Timer agentLatencyTimer;
    private final InputValidator inputValidator;

    private final AgentMetricsDashboard metricsDashboard;

    public AgentObservationService(ChatClient.Builder builder,
                                   ObservationRegistry observationRegistry,
                                   TokenUsageMetrics tokenUsageMetrics,
                                   MeterRegistry meterRegistry,
                                   InputValidator inputValidator,
                                   ObservabilityAdvisor observabilityAdvisor,
                                   AgentMetricsDashboard metricsDashboard) {
        this.observationRegistry = observationRegistry;
        this.tokenUsageMetrics = tokenUsageMetrics;
        this.inputValidator = inputValidator;
        this.metricsDashboard = metricsDashboard;

        /*
         * Spring AI 1.0 auto-instruments ChatClient calls with Micrometer Observations.
         * Each LLM call emits:
         *   - Span: "spring.ai.chat" with attributes: model, prompt token count, completion token count
         *   - Metric: gen_ai.client.token.usage (counter, split by token_type)
         *   - Metric: gen_ai.client.operation.duration (timer histogram)
         *
         * Our custom ObservabilityAdvisor adds:
         *   - llm.calls.total / llm.errors.total counters
         *   - Per-call span with status and error attributes
         *
         * SimpleLoggerAdvisor logs every request/response at DEBUG level.
         * Order matters: ObservabilityAdvisor runs first (lowest order = outermost).
         */
        this.chatClient = builder
                .defaultSystem("You are a helpful assistant.")
                .defaultAdvisors(observabilityAdvisor, new SimpleLoggerAdvisor())
                .build();

        this.agentLatencyTimer = Timer.builder("agent.request.duration")
                .description("End-to-end agent request latency including LLM call")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
    }

    public String chat(String userId, String message) {
        var validation = inputValidator.validate(message);
        if (!validation.valid()) {
            metricsDashboard.recordGuardrailRejection();
            throw new IllegalArgumentException(validation.reason());
        }

        var timerSample = metricsDashboard.startRequestTimer();

        /*
         * Wrapping in an Observation creates a custom span in the distributed trace.
         * The HTTP request span is automatically the parent — Micrometer propagates the
         * trace context. In Jaeger you see: HTTP POST → agent.chat → llm.call → spring.ai.chat
         *
         * lowCardinalityKeyValue = becomes a span attribute AND a Prometheus label.
         * Keep cardinality low: user.id is OK (bounded set); do NOT add message content.
         */
        return Observation.createNotStarted("agent.chat", observationRegistry)
                .lowCardinalityKeyValue("user.id", userId)
                .observe(() -> agentLatencyTimer.record(() -> {
                    String reply = chatClient.prompt().user(message).call().content();

                    // Approximate token counts (4 chars ≈ 1 token for English text)
                    long promptTokens = message.length() / 4;
                    long completionTokens = reply == null ? 0 : reply.length() / 4;
                    tokenUsageMetrics.record(promptTokens, completionTokens);
                    metricsDashboard.recordTokenUsage(promptTokens, completionTokens);
                    metricsDashboard.stopRequestTimer(timerSample);

                    return reply;
                }));
    }
}
