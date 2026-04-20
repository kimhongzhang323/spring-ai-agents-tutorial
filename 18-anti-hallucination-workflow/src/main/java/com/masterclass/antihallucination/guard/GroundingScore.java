package com.masterclass.antihallucination.guard;

/**
 * Structured output returned by the judge LLM.
 *
 * @param faithfulness 0–1: is the answer grounded in the provided documents?
 * @param confidence   0–1: how certain is the judge about this score?
 * @param explanation  one-sentence reason; surfaced to the caller in {@link com.masterclass.antihallucination.domain.StepResult}
 */
public record GroundingScore(
        double faithfulness,
        double confidence,
        String explanation
) {
    public boolean passes(double faithfulnessThreshold, double confidenceThreshold) {
        return faithfulness >= faithfulnessThreshold && confidence >= confidenceThreshold;
    }
}
