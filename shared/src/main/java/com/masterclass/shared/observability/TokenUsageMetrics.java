package com.masterclass.shared.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class TokenUsageMetrics {

    private final Counter promptTokenCounter;
    private final Counter completionTokenCounter;
    private final Counter requestCounter;

    public TokenUsageMetrics(MeterRegistry registry) {
        promptTokenCounter = Counter.builder("llm.tokens.prompt")
                .description("Total prompt tokens sent to LLM")
                .register(registry);
        completionTokenCounter = Counter.builder("llm.tokens.completion")
                .description("Total completion tokens received from LLM")
                .register(registry);
        requestCounter = Counter.builder("llm.requests.total")
                .description("Total LLM requests")
                .register(registry);
    }

    public void record(int promptTokens, int completionTokens) {
        promptTokenCounter.increment(promptTokens);
        completionTokenCounter.increment(completionTokens);
        requestCounter.increment();
    }
}
