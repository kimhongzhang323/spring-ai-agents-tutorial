package com.masterclass.lc4jagentic;

public record CritiqueResult(
        String feedback,
        String verdict   // "APPROVED" or "NEEDS_REVISION"
) {
    public boolean approved() {
        return "APPROVED".equalsIgnoreCase(verdict);
    }
}
