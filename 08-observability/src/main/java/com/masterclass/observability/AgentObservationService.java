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

    public AgentObservationService(ChatClient.Builder builder,
                                   ObservationRegistry observationRegistry,
                                   TokenUsageMetrics tokenUsageMetrics,
                                   MeterRegistry meterRegistry,
                                   InputValidator inputValidator) {
        this.observationRegistry = observationRegistry;
        this.tokenUsageMetrics = tokenUsageMetrics;
        this.inputValidator = inputValidator;
        /*
         * Spring AI 1.0 auto-instruments ChatClient calls with Micrometer Observations.
         * Each LLM call emits:
         *   - Span: "spring.ai.chat" with attributes: model, prompt token count, completion token count
         *   - Metric: gen_ai.client.token.usage (counter)
         *   - Metric: gen_ai.client.operation.duration (timer)
         *
         * SimpleLoggerAdvisor additionally logs every request/response at DEBUG.
         */
        this.chatClient = builder
                .defaultSystem("You are a helpful assistant.")
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();

        this.agentLatencyTimer = Timer.builder("agent.request.duration")
                .description("End-to-end agent request latency including LLM call")
                .register(meterRegistry);
    }

    public String chat(String userId, String message) {
        var validation = inputValidator.validate(message);
        if (!validation.valid()) throw new IllegalArgumentException(validation.reason());

        /*
         * Wrapping in an Observation adds a custom span to the trace.
         * The parent span (HTTP request) is automatically linked by Micrometer.
         * In Jaeger you will see: HTTP GET → agent.chat → spring.ai.chat (LLM call)
         */
        return Observation.createNotStarted("agent.chat", observationRegistry)
                .lowCardinalityKeyValue("user.id", userId)
                .observe(() -> agentLatencyTimer.record(() -> {
                    String reply = chatClient.prompt().user(message).call().content();
                    // Record to shared TokenUsageMetrics — approximate token counts
                    tokenUsageMetrics.record(message.length() / 4, reply == null ? 0 : reply.length() / 4);
                    return reply;
                }));
    }
}
