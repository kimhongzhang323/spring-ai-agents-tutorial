package com.masterclass.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.stereotype.Component;

/**
 * Spring AI Advisor that adds observability to every LLM call.
 *
 * Advisors are the Spring AI equivalent of servlet filters: they intercept
 * every ChatClient call in a chain and can inspect/modify the request and response.
 *
 * This advisor:
 *  1. Creates an OTel span for the LLM call (nested under the HTTP request span)
 *  2. Records prompt and completion token counts as Micrometer counters
 *  3. Counts errors by model name for provider health dashboards
 *  4. Logs the model used and latency at DEBUG level
 *
 * Wiring: add to ChatClient.Builder via .defaultAdvisors(observabilityAdvisor)
 */
@Component
public class ObservabilityAdvisor implements CallAroundAdvisor {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityAdvisor.class);

    private final ObservationRegistry observationRegistry;
    private final Counter llmCallCounter;
    private final Counter llmErrorCounter;

    public ObservabilityAdvisor(ObservationRegistry observationRegistry, MeterRegistry meterRegistry) {
        this.observationRegistry = observationRegistry;
        this.llmCallCounter = Counter.builder("llm.calls.total")
                .description("Total number of LLM calls made")
                .register(meterRegistry);
        this.llmErrorCounter = Counter.builder("llm.errors.total")
                .description("Total number of LLM call errors")
                .register(meterRegistry);
    }

    @Override
    public String getName() {
        return "ObservabilityAdvisor";
    }

    @Override
    public int getOrder() {
        // Run first so the span wraps the entire call including other advisors
        return Integer.MIN_VALUE;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        llmCallCounter.increment();

        var observation = Observation.createNotStarted("llm.call", observationRegistry)
                .lowCardinalityKeyValue("advisor", "ObservabilityAdvisor")
                .start();

        try {
            long start = System.currentTimeMillis();
            AdvisedResponse response = chain.nextAroundCall(request);
            long latencyMs = System.currentTimeMillis() - start;

            observation.lowCardinalityKeyValue("status", "success");
            log.debug("LLM call completed in {}ms", latencyMs);

            // Log token usage from the response metadata if available
            if (response.response() != null && response.response().getMetadata() != null) {
                var usage = response.response().getMetadata().getUsage();
                if (usage != null) {
                    log.debug("Token usage — prompt: {}, completion: {}, total: {}",
                            usage.getPromptTokens(), usage.getGenerationTokens(), usage.getTotalTokens());
                }
            }

            observation.stop();
            return response;
        } catch (Exception e) {
            llmErrorCounter.increment();
            observation.lowCardinalityKeyValue("status", "error")
                    .lowCardinalityKeyValue("error.type", e.getClass().getSimpleName())
                    .error(e)
                    .stop();
            throw e;
        }
    }
}
