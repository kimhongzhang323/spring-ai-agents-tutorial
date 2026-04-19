package com.masterclass.microservices.orchestrator;

import java.time.Instant;

public record AgentResponse(
        String response,
        String integrationUsed,
        long promptTokens,
        long completionTokens,
        Instant timestamp
) {
    public static AgentResponse of(String response, String integration,
                                   long prompt, long completion) {
        return new AgentResponse(response, integration, prompt, completion, Instant.now());
    }
}
