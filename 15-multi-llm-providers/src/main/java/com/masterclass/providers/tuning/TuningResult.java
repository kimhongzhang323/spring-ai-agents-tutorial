package com.masterclass.providers.tuning;

public record TuningResult(
        String response,
        String providerUsed,
        ResponseConfig configApplied,
        long latencyMs
) {}
