package com.masterclass.antihallucination.domain;

/**
 * Result of a single workflow step, emitted as an SSE event.
 *
 * @param stepName        matches {@link StepDef#name()}
 * @param status          outcome of the step
 * @param output          LLM-generated text (null for GUARD/TRANSFORM steps)
 * @param faithfulness    grounding faithfulness score (null when guard not run)
 * @param confidence      grounding confidence score (null when guard not run)
 * @param guardExplanation human-readable explanation from the judge (null when guard not run)
 * @param durationMs      wall-clock time for this step in milliseconds
 */
public record StepResult(
        String stepName,
        StepStatus status,
        String output,
        Double faithfulness,
        Double confidence,
        String guardExplanation,
        long durationMs
) {}
