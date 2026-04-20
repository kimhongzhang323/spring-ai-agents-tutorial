package com.masterclass.antihallucination.domain;

import java.util.List;

/**
 * Declarative definition of a single workflow step submitted by the caller.
 *
 * @param name            unique identifier used as context key for the step output
 * @param type            execution mode for this step
 * @param promptTemplate  SpEL-style template; use {{contextKey}} to interpolate prior step outputs
 * @param groundingDocs   verbatim text fragments used by the HallucinationGuard to verify faithfulness
 * @param consistencySamples number of LLM samples for majority vote (1 = disabled)
 * @param guardEnabled    whether to run the inline hallucination guard after the LLM call
 * @param targetContextKey the context key whose value this GUARD step should verify (GUARD type only)
 */
public record StepDef(
        String name,
        StepType type,
        String promptTemplate,
        List<String> groundingDocs,
        int consistencySamples,
        boolean guardEnabled,
        String targetContextKey
) {
    public StepDef {
        if (consistencySamples < 1) consistencySamples = 1;
        if (groundingDocs == null) groundingDocs = List.of();
    }
}
