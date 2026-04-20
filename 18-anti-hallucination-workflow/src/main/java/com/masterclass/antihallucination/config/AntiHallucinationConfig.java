package com.masterclass.antihallucination.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * All thresholds and tuning parameters loaded from application.yml.
 * Centralised here so nothing is hardcoded in service classes.
 */
@ConfigurationProperties(prefix = "anti-hallucination")
public record AntiHallucinationConfig(
        GuardConfig guard,
        SelfConsistencyConfig selfConsistency,
        WorkflowConfig workflow
) {
    public record GuardConfig(
            double faithfulnessThreshold,
            double confidenceThreshold,
            String judgeModel,
            int maxRetries
    ) {}

    public record SelfConsistencyConfig(
            int defaultSamples,
            double temperature
    ) {}

    public record WorkflowConfig(
            int humanPauseTimeoutSeconds,
            int contextTtlSeconds
    ) {}
}
