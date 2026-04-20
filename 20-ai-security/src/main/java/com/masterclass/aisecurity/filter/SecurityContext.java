package com.masterclass.aisecurity.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable context passed through the SecurityGateway filter chain.
 * Each filter may sanitize the input or append warnings.
 */
public record SecurityContext(
        String userId,
        String originalInput,
        String sanitizedInput,
        double riskScore,
        List<String> warnings,
        boolean blocked
) {
    public static SecurityContext of(String userId, String input) {
        return new SecurityContext(userId, input, input, 0.0, new ArrayList<>(), false);
    }

    public SecurityContext withSanitized(String sanitized) {
        return new SecurityContext(userId, originalInput, sanitized, riskScore, warnings, blocked);
    }

    public SecurityContext withRisk(double score) {
        return new SecurityContext(userId, originalInput, sanitizedInput, score, warnings, blocked);
    }

    public SecurityContext withWarning(String warning) {
        warnings.add(warning);
        return this;
    }

    public SecurityContext blocked() {
        return new SecurityContext(userId, originalInput, sanitizedInput, riskScore, warnings, true);
    }
}
