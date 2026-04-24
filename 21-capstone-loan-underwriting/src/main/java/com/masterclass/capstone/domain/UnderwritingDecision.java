package com.masterclass.capstone.domain;

import java.time.Instant;
import java.util.List;

public record UnderwritingDecision(
        String jobId,
        String applicantId,
        Outcome outcome,
        double approvedAmount,
        double approvedRatePercent,
        List<RationaleItem> rationale,
        List<Finding> evidence,
        Instant decidedAt
) {
    public enum Outcome { APPROVED, CONDITIONALLY_APPROVED, DECLINED, REFER_TO_HUMAN }

    /** A single supervisor conclusion that must cite one or more finding IDs. */
    public record RationaleItem(String statement, List<String> citedFindingIds) {}
}
