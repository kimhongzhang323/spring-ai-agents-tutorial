package com.masterclass.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.stereotype.Component;

/**
 * Advisor that enforces a per-request token budget and records over-budget events.
 *
 * <h2>Why token budgets matter in production</h2>
 * Without budgets, a single malicious or buggy request could:
 * <ul>
 *   <li>Send a 100K-token prompt (expensive, slow, and may violate provider limits)</li>
 *   <li>Trigger a tool loop that keeps calling back and forth indefinitely</li>
 *   <li>Exhaust the monthly API quota in minutes</li>
 * </ul>
 *
 * <h2>Two enforcement points</h2>
 * <ol>
 *   <li><strong>Input budget</strong>: check prompt length BEFORE calling the LLM.
 *       Reject if estimated prompt tokens exceed {@code maxInputTokens}.
 *       Estimation: {@code charCount / 4} (rough English approximation).</li>
 *   <li><strong>Output budget</strong>: check completion tokens AFTER the LLM responds.
 *       Log a warning and record a metric if the response exceeded expectations.
 *       The response is returned as-is (we can't truncate mid-sentence).</li>
 * </ol>
 *
 * <h2>Wiring order</h2>
 * This advisor should run BEFORE {@link ObservabilityAdvisor} so that over-budget
 * requests are rejected before any spans or counters are emitted for them.
 * Use {@code getOrder()} returning a value lower than {@link ObservabilityAdvisor}'s.
 *
 * <h2>Production enhancement</h2>
 * For accurate token counting, integrate tiktoken-java (for OpenAI models) or
 * the provider's tokenizer API. Character-based estimation is a pragmatic approximation
 * that avoids an extra API call per request.
 */
@Component
public class TokenBudgetAdvisor implements CallAroundAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TokenBudgetAdvisor.class);

    private final int maxInputTokens;
    private final int maxOutputTokens;
    private final Counter inputBudgetExceededCounter;
    private final Counter outputBudgetExceededCounter;

    public TokenBudgetAdvisor(MeterRegistry meterRegistry) {
        // Configurable in production via @ConfigurationProperties
        this.maxInputTokens = 8_000;
        this.maxOutputTokens = 2_000;

        this.inputBudgetExceededCounter = Counter.builder("llm.budget.exceeded")
                .tag("direction", "input")
                .description("Requests rejected due to oversized input prompt")
                .register(meterRegistry);

        this.outputBudgetExceededCounter = Counter.builder("llm.budget.exceeded")
                .tag("direction", "output")
                .description("Responses that exceeded the expected output token budget")
                .register(meterRegistry);
    }

    @Override
    public String getName() {
        return "TokenBudgetAdvisor";
    }

    @Override
    public int getOrder() {
        // Run before ObservabilityAdvisor (MIN_VALUE) — reject before span is created
        return Integer.MIN_VALUE + 1;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        // Estimate input token count from all messages in the request
        long estimatedInputTokens = estimateTokens(request);

        if (estimatedInputTokens > maxInputTokens) {
            inputBudgetExceededCounter.increment();
            log.warn("Input budget exceeded: estimated {} tokens > max {} tokens. Rejecting request.",
                    estimatedInputTokens, maxInputTokens);
            throw new TokenBudgetExceededException(
                    "Request exceeds input token budget (%d estimated > %d max). Please shorten your message."
                            .formatted(estimatedInputTokens, maxInputTokens));
        }

        AdvisedResponse response = chain.nextAroundCall(request);

        // Post-call: check output token usage from metadata
        if (response.response() != null && response.response().getMetadata() != null) {
            var usage = response.response().getMetadata().getUsage();
            if (usage != null && usage.getGenerationTokens() != null) {
                long outputTokens = usage.getGenerationTokens();
                if (outputTokens > maxOutputTokens) {
                    outputBudgetExceededCounter.increment();
                    log.warn("Output budget exceeded: {} tokens generated > {} max. Consider prompt engineering.",
                            outputTokens, maxOutputTokens);
                    // We log and record the metric but do NOT throw — the answer has already been generated.
                    // To enforce hard output limits, set max_tokens in the model options instead.
                }
            }
        }

        return response;
    }

    /**
     * Estimates total input tokens by summing character counts of all messages.
     * Rule of thumb: 1 token ≈ 4 characters for English prose.
     * For production accuracy, use a provider-specific tokenizer.
     */
    private long estimateTokens(AdvisedRequest request) {
        long charCount = 0;
        if (request.systemText() != null) charCount += request.systemText().length();
        if (request.userText() != null) charCount += request.userText().length();
        if (request.messages() != null) {
            charCount += request.messages().stream()
                    .mapToLong(m -> m.getText() == null ? 0 : m.getText().length())
                    .sum();
        }
        return charCount / 4;
    }

    public static class TokenBudgetExceededException extends RuntimeException {
        public TokenBudgetExceededException(String message) {
            super(message);
        }
    }
}
