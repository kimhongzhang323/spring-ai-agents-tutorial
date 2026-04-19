package com.masterclass.knowledgegraph.graph;

/**
 * Thrown by a node to pause execution and request human approval before
 * continuing. The engine catches this, persists state, and returns a
 * SUSPENDED run result. The caller can resume by supplying the human
 * response and calling GraphEngine#resume.
 */
public class InterruptException extends RuntimeException {

    private final String prompt;

    public InterruptException(String prompt) {
        super("Graph interrupted: " + prompt);
        this.prompt = prompt;
    }

    public String prompt() {
        return prompt;
    }
}
