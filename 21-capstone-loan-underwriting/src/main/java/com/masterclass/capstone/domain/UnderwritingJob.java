package com.masterclass.capstone.domain;

import java.time.Instant;

public record UnderwritingJob(
        String jobId,
        String applicantId,
        String officerId,
        LoanApplication application,
        Status status,
        UnderwritingDecision decision,
        String failureReason,
        Instant createdAt,
        Instant completedAt
) {
    public enum Status { RUNNING, COMPLETED, FAILED, REJECTED_BY_GUARDRAIL }

    public static UnderwritingJob create(String jobId, String officerId, LoanApplication app) {
        return new UnderwritingJob(jobId, app.applicantId(), officerId, app,
                Status.RUNNING, null, null, Instant.now(), null);
    }

    public UnderwritingJob completed(UnderwritingDecision d) {
        return new UnderwritingJob(jobId, applicantId, officerId, application,
                Status.COMPLETED, d, null, createdAt, Instant.now());
    }

    public UnderwritingJob failed(String reason) {
        return new UnderwritingJob(jobId, applicantId, officerId, application,
                Status.FAILED, null, reason, createdAt, Instant.now());
    }

    public UnderwritingJob rejectedByGuardrail(String reason) {
        return new UnderwritingJob(jobId, applicantId, officerId, application,
                Status.REJECTED_BY_GUARDRAIL, null, reason, createdAt, Instant.now());
    }
}
