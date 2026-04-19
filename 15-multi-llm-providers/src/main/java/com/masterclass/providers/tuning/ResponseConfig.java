package com.masterclass.providers.tuning;

import java.util.List;

/**
 * All tunable parameters for a single LLM call.
 *
 * temperature:    0.0–2.0. Lower = focused/deterministic. Higher = creative/varied.
 *                 Rule of thumb: 0.0–0.3 for factual, 0.7 for chat, 1.0+ for creative.
 *
 * maxTokens:      Hard cap on output tokens. 1 token ≈ 0.75 English words.
 *                 Set this to control cost and latency on known-length tasks.
 *
 * topP:           Nucleus sampling. Only consider tokens whose cumulative probability
 *                 reaches topP. 0.9 is a safe default. Lower = less variety.
 *                 Don't set both topP and temperature — pick one.
 *
 * topK:           Limit vocabulary to the K most likely tokens at each step.
 *                 Smaller K = less variance. Not all providers support this.
 *
 * stopSequences:  The model stops generating when it outputs any of these strings.
 *                 Useful for structured extraction ("stop at '}' after JSON").
 *
 * systemPrompt:   Override the default system prompt for this request only.
 */
public record ResponseConfig(
        Double temperature,
        Integer maxTokens,
        Double topP,
        Integer topK,
        java.util.List<String> stopSequences,
        String systemPrompt
) {
    /** Sensible defaults for a typical chat use case. */
    public static ResponseConfig defaults() {
        return new ResponseConfig(0.7, 1024, null, null, List.of(), null);
    }

    /** Low temperature, short output — ideal for factual/classification tasks. */
    public static ResponseConfig factual() {
        return new ResponseConfig(0.1, 512, null, null, List.of(), null);
    }

    /** High temperature, long output — ideal for creative writing. */
    public static ResponseConfig creative() {
        return new ResponseConfig(1.0, 2048, null, null, List.of(), null);
    }
}
