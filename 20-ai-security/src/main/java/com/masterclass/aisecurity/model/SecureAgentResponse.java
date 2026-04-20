package com.masterclass.aisecurity.model;

import java.time.Instant;
import java.util.List;

public record SecureAgentResponse(
        String response,
        List<String> warnings,
        Instant processedAt
) {
    public static SecureAgentResponse of(String response, List<String> warnings) {
        return new SecureAgentResponse(response, warnings, Instant.now());
    }
}
