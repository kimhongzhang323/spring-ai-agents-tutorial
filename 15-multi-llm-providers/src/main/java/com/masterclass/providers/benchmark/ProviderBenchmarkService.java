package com.masterclass.providers.benchmark;

import com.masterclass.providers.router.ProviderRouter;
import com.masterclass.providers.router.RoutingStrategy;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Runs a prompt against multiple providers in parallel and compares results.
 *
 * Use this to:
 * - Choose the right model for a new use case
 * - Validate that a cheaper provider meets quality requirements
 * - Detect provider degradation in production
 *
 * Results include latency, response quality (raw text), and estimated cost.
 */
@Service
public class ProviderBenchmarkService {

    private static final Logger log = LoggerFactory.getLogger(ProviderBenchmarkService.class);

    // Approximate input cost per 1M tokens in USD (as of 2025 — check current pricing)
    private static final Map<String, Double> COST_PER_MILLION_TOKENS = Map.of(
            "openai",    2.50,
            "anthropic", 0.80,
            "gemini",    0.075,
            "groq",      0.05,
            "mistral",   0.10,
            "bedrock",   0.80,
            "azure",     2.50,
            "ollama",    0.0
    );

    private final ProviderRouter router;
    private final MeterRegistry meterRegistry;

    public ProviderBenchmarkService(ProviderRouter router, MeterRegistry meterRegistry) {
        this.router = router;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Executes the prompt against all specified providers in parallel.
     * Returns a BenchmarkReport with latency, responses, and cost estimates.
     */
    public BenchmarkReport benchmark(String prompt, List<String> providerNames) {
        var available = router.availableProviders();
        List<String> targets = providerNames.isEmpty()
                ? List.copyOf(available)
                : providerNames.stream().filter(available::contains).toList();

        log.info("Benchmarking {} providers with prompt: {}", targets.size(), prompt.substring(0, Math.min(50, prompt.length())));

        // Execute all providers in parallel
        List<CompletableFuture<BenchmarkReport.ProviderResult>> futures = targets.stream()
                .map(name -> CompletableFuture.supplyAsync(() -> callProvider(name, prompt)))
                .toList();

        List<BenchmarkReport.ProviderResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        String fastest = results.stream()
                .filter(r -> "SUCCESS".equals(r.status()))
                .min(Comparator.comparingLong(BenchmarkReport.ProviderResult::latencyMs))
                .map(BenchmarkReport.ProviderResult::provider)
                .orElse("none");

        String bestCost = results.stream()
                .filter(r -> "SUCCESS".equals(r.status()))
                .min(Comparator.comparingDouble(BenchmarkReport.ProviderResult::estimatedCostUsd))
                .map(BenchmarkReport.ProviderResult::provider)
                .orElse("none");

        return new BenchmarkReport(prompt, results, fastest, bestCost);
    }

    private BenchmarkReport.ProviderResult callProvider(String providerName, String prompt) {
        ChatClient client = router.select(RoutingStrategy.EXPLICIT, providerName);
        Timer timer = Timer.builder("benchmark.provider.latency")
                .tag("provider", providerName)
                .register(meterRegistry);

        long start = System.currentTimeMillis();
        try {
            String response = timer.record(() -> client.prompt().user(prompt).call().content());
            long latencyMs = System.currentTimeMillis() - start;
            double costEstimate = estimateCost(prompt, response, providerName);

            log.debug("Benchmark [{}] completed in {}ms", providerName, latencyMs);
            return new BenchmarkReport.ProviderResult(providerName, response, latencyMs, "SUCCESS", null, costEstimate);
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - start;
            log.warn("Benchmark [{}] failed: {}", providerName, e.getMessage());
            return new BenchmarkReport.ProviderResult(providerName, null, latencyMs, "ERROR", e.getMessage(), 0.0);
        }
    }

    private double estimateCost(String prompt, String response, String provider) {
        // Very rough estimate: count words * 1.3 ≈ tokens
        int estimatedTokens = (int) ((prompt.split("\\s+").length + (response != null ? response.split("\\s+").length : 0)) * 1.3);
        double costPerMillion = COST_PER_MILLION_TOKENS.getOrDefault(provider, 1.0);
        return (estimatedTokens / 1_000_000.0) * costPerMillion;
    }
}
