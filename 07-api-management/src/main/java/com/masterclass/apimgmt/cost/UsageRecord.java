package com.masterclass.apimgmt.cost;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usage_records", indexes = {
        @Index(name = "idx_usage_user", columnList = "userId"),
        @Index(name = "idx_usage_timestamp", columnList = "timestamp")
})
public class UsageRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private String model;
    private int promptTokens;
    private int completionTokens;
    private double estimatedCostUsd;
    private Instant timestamp;

    protected UsageRecord() {}

    public UsageRecord(String userId, String model, int promptTokens, int completionTokens,
                       double estimatedCostUsd, Instant timestamp) {
        this.userId = userId;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.estimatedCostUsd = estimatedCostUsd;
        this.timestamp = timestamp;
    }

    public String getUserId() { return userId; }
    public String getModel() { return model; }
    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public double getEstimatedCostUsd() { return estimatedCostUsd; }
    public Instant getTimestamp() { return timestamp; }
}
