package com.masterclass.capstone.domain;

/**
 * An atomic evidence fact produced by a specialist agent or tool.
 * The id (e.g. CR-001, FR-002) is what the supervisor must cite in its rationale.
 * Keeping findings typed and id-addressable is what makes citation validation possible.
 */
public record Finding(
        String id,
        String source,       // CREDIT | FRAUD | INCOME | COMPLIANCE | POLICY
        String statement,
        Severity severity
) {
    public enum Severity { INFO, POSITIVE, WARNING, BLOCKER }
}
