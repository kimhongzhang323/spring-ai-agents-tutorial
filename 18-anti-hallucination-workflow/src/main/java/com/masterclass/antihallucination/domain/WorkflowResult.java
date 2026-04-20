package com.masterclass.antihallucination.domain;

import java.util.List;

/**
 * Final summary emitted as the last SSE event when the workflow completes.
 */
public record WorkflowResult(
        String workflowName,
        StepStatus overallStatus,
        List<StepResult> steps,
        int passedSteps,
        int failedSteps,
        long totalDurationMs
) {
    public static WorkflowResult from(String name, List<StepResult> results, long totalDurationMs) {
        int passed = (int) results.stream().filter(r -> r.status() == StepStatus.PASSED).count();
        int failed = (int) results.stream().filter(r -> r.status() == StepStatus.FAILED).count();
        StepStatus overall = failed > 0 ? StepStatus.FAILED : StepStatus.PASSED;
        return new WorkflowResult(name, overall, results, passed, failed, totalDurationMs);
    }
}
