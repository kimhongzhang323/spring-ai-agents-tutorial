package com.masterclass.shared.dto;

public record AgentResponse(
        String message,
        String conversationId,
        TokenUsage tokenUsage
) {
    public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {}

    public static AgentResponse of(String message) {
        return new AgentResponse(message, null, null);
    }

    public static AgentResponse of(String message, String conversationId) {
        return new AgentResponse(message, conversationId, null);
    }

    public static AgentResponse of(String message, String conversationId, TokenUsage usage) {
        return new AgentResponse(message, conversationId, usage);
    }
}
