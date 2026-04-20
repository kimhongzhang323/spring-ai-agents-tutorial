package com.masterclass.antihallucination.domain;

import java.util.List;

/**
 * Top-level workflow submitted via the REST API.
 *
 * @param name  human-readable workflow name (used in SSE event metadata)
 * @param steps ordered list of steps; executed sequentially
 * @param abortOnFirstFailure stop the workflow on the first FAILED step when true
 */
public record WorkflowDef(
        String name,
        List<StepDef> steps,
        boolean abortOnFirstFailure
) {}
