package com.masterclass.llmbasics.common;

/**
 * Unified response wrapper so every provider demo returns the same shape.
 */
public record LlmResponse(
        String provider,
        String model,
        String content,
        int promptTokens,
        int completionTokens
) {
    @Override
    public String toString() {
        return """
                ── %s (%s) ──
                %s
                [tokens: prompt=%d, completion=%d]
                """.formatted(provider, model, content, promptTokens, completionTokens);
    }
}
