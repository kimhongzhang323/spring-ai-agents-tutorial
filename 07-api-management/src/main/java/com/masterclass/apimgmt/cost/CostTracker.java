package com.masterclass.apimgmt.cost;

import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Records token usage per user per request.
 * Deep-dive of the skeleton in shared/ — now wired to JPA for persistence.
 *
 * In production: consider a time-series DB (InfluxDB, TimescaleDB) or
 * stream to Kafka for real-time billing dashboards.
 */
@Service
public class CostTracker {

    private static final Logger log = LoggerFactory.getLogger(CostTracker.class);

    // Approximate cost per 1000 tokens (USD) — update these for your model
    public enum ModelPricing {
        GPT_4O_MINI(0.00015, 0.00060),
        GPT_4O    (0.00500, 0.01500),
        LLAMA_3_1 (0.0,     0.0);    // local model — no API cost

        final double promptPer1k;
        final double completionPer1k;
        ModelPricing(double promptPer1k, double completionPer1k) {
            this.promptPer1k = promptPer1k;
            this.completionPer1k = completionPer1k;
        }
    }

    private final UsageRecordRepository repository;

    public CostTracker(UsageRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(String userId, String model, int promptTokens, int completionTokens) {
        double cost = estimateCost(model, promptTokens, completionTokens);
        var record = new UsageRecord(userId, model, promptTokens, completionTokens, cost, Instant.now());
        repository.save(record);
        log.debug("Recorded {} prompt + {} completion tokens for user {} (${} estimated)",
                promptTokens, completionTokens, userId, String.format("%.6f", cost));
    }

    public List<UsageRecord> getUsageForUser(String userId) {
        return repository.findByUserIdOrderByTimestampDesc(userId);
    }

    public UserUsageSummary getSummary(String userId) {
        return repository.summarizeForUser(userId);
    }

    private double estimateCost(String modelName, int prompt, int completion) {
        ModelPricing pricing = ModelPricing.LLAMA_3_1;
        if (modelName != null) {
            if (modelName.contains("gpt-4o-mini")) pricing = ModelPricing.GPT_4O_MINI;
            else if (modelName.contains("gpt-4o"))  pricing = ModelPricing.GPT_4O;
        }
        return (prompt / 1000.0) * pricing.promptPer1k + (completion / 1000.0) * pricing.completionPer1k;
    }

    public record UserUsageSummary(String userId, long totalRequests, long totalPromptTokens,
                                   long totalCompletionTokens, double totalCostUsd) {}
}
