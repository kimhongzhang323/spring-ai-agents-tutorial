package com.masterclass.providers.fallback;

public record FallbackResult(
        String response,
        String providerUsed,
        int attemptsNeeded,
        String errorMessage
) {
    public boolean succeeded() {
        return errorMessage == null;
    }
}
