package com.masterclass.providers.benchmark;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BenchmarkReport(
        String prompt,
        List<ProviderResult> results,
        String fastestProvider,
        String bestCostProvider
) {
    public record ProviderResult(
            String provider,
            String response,
            long latencyMs,
            String status,
            String errorMessage,
            /** Estimated cost in USD per 1000 tokens — approximate, from public pricing */
            double estimatedCostUsd
    ) {}
}
