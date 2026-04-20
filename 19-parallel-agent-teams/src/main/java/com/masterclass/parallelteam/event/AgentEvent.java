package com.masterclass.parallelteam.event;

import java.time.Instant;

/**
 * Sealed hierarchy for typed inter-agent events published on the AgentEventBus.
 * Each record carries the job ID so consumers can correlate events across parallel runs.
 */
public sealed interface AgentEvent permits
        AgentEvent.ResearchCompleted,
        AgentEvent.AnalysisCompleted,
        AgentEvent.CitationCompleted,
        AgentEvent.SynthesisCompleted,
        AgentEvent.AgentFailed {

    String jobId();
    Instant occurredAt();

    record ResearchCompleted(String jobId, String facts, Instant occurredAt) implements AgentEvent {}
    record AnalysisCompleted(String jobId, String trends, Instant occurredAt) implements AgentEvent {}
    record CitationCompleted(String jobId, String citations, Instant occurredAt) implements AgentEvent {}
    record SynthesisCompleted(String jobId, String report, Instant occurredAt) implements AgentEvent {}
    record AgentFailed(String jobId, String agentName, String reason, Instant occurredAt) implements AgentEvent {}
}
