package com.masterclass.aisecurity.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "security_audit")
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;
    private String sanitizedInput;     // PII already redacted before storage
    private double riskScore;

    @Enumerated(EnumType.STRING)
    private AuditOutcome outcome;

    private String outcomeDetail;
    private Instant occurredAt;

    public enum AuditOutcome { ALLOWED, ALLOWED_WITH_WARNINGS, BLOCKED }

    protected AuditRecord() {}

    public AuditRecord(String userId, String sanitizedInput, double riskScore,
                       AuditOutcome outcome, String outcomeDetail) {
        this.userId = userId;
        this.sanitizedInput = sanitizedInput;
        this.riskScore = riskScore;
        this.outcome = outcome;
        this.outcomeDetail = outcomeDetail;
        this.occurredAt = Instant.now();
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getSanitizedInput() { return sanitizedInput; }
    public double getRiskScore() { return riskScore; }
    public AuditOutcome getOutcome() { return outcome; }
    public String getOutcomeDetail() { return outcomeDetail; }
    public Instant getOccurredAt() { return occurredAt; }
}
