package com.masterclass.aisecurity.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable context passed through the SecurityGateway filter chain.
 * Every mutation returns a new instance — the warnings list is never shared.
 */
public record SecurityContext(
        String userId,
        String originalInput,
        String sanitizedInput,
        double riskScore,
        List<String> warnings,
        boolean blocked
) {
    /** Canonical constructor: defensive copy so callers cannot mutate our list. */
    public SecurityContext {
        warnings = List.copyOf(warnings);
    }

    public static SecurityContext of(String userId, String input) {
        return new SecurityContext(userId, input, input, 0.0, List.of(), false);
    }

    public SecurityContext withSanitized(String sanitized) {
        return new SecurityContext(userId, originalInput, sanitized, riskScore, warnings, blocked);
    }

    public SecurityContext withRisk(double score) {
        return new SecurityContext(userId, originalInput, sanitizedInput, score, warnings, blocked);
    }

    /** Returns a new context with the warning appended — does NOT mutate this instance. */
    public SecurityContext withWarning(String warning) {
        List<String> updated = new ArrayList<>(warnings);
        updated.add(warning);
        return new SecurityContext(userId, originalInput, sanitizedInput, riskScore, updated, blocked);
    }

    public SecurityContext blocked() {
        return new SecurityContext(userId, originalInput, sanitizedInput, riskScore, warnings, true);
    }
}
