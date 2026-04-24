package com.masterclass.capstone.event;

import com.masterclass.capstone.domain.Finding;
import com.masterclass.capstone.domain.UnderwritingDecision;

import java.time.Instant;
import java.util.List;

public sealed interface UnderwritingEvent permits
        UnderwritingEvent.CreditCompleted,
        UnderwritingEvent.FraudCompleted,
        UnderwritingEvent.IncomeCompleted,
        UnderwritingEvent.ComplianceCompleted,
        UnderwritingEvent.DecisionMade,
        UnderwritingEvent.DecisionRejected,
        UnderwritingEvent.AgentFailed {

    String jobId();
    Instant occurredAt();

    record CreditCompleted(String jobId, List<Finding> findings, Instant occurredAt) implements UnderwritingEvent {}
    record FraudCompleted(String jobId, List<Finding> findings, Instant occurredAt) implements UnderwritingEvent {}
    record IncomeCompleted(String jobId, List<Finding> findings, Instant occurredAt) implements UnderwritingEvent {}
    record ComplianceCompleted(String jobId, List<Finding> findings, Instant occurredAt) implements UnderwritingEvent {}
    record DecisionMade(String jobId, UnderwritingDecision decision, Instant occurredAt) implements UnderwritingEvent {}
    record DecisionRejected(String jobId, String reason, Instant occurredAt) implements UnderwritingEvent {}
    record AgentFailed(String jobId, String agentName, String reason, Instant occurredAt) implements UnderwritingEvent {}
}
