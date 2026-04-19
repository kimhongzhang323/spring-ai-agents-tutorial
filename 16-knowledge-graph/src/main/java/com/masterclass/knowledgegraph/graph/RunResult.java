package com.masterclass.knowledgegraph.graph;

/**
 * Result returned by GraphEngine after a run or resume.
 *
 * @param status        COMPLETED, SUSPENDED (interrupt), or ERROR
 * @param finalState    state at the point the run stopped
 * @param interruptPrompt  non-null only when status == SUSPENDED
 * @param error         non-null only when status == ERROR
 * @param threadId      opaque ID for resuming a suspended run
 */
public record RunResult(
        Status status,
        GraphState finalState,
        String interruptPrompt,
        String error,
        String threadId
) {
    public enum Status { COMPLETED, SUSPENDED, ERROR }

    public boolean isCompleted()  { return status == Status.COMPLETED; }
    public boolean isSuspended()  { return status == Status.SUSPENDED; }
}
