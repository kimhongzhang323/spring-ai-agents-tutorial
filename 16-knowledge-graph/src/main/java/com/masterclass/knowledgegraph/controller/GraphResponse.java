package com.masterclass.knowledgegraph.controller;

import com.masterclass.knowledgegraph.graph.RunResult;

import java.util.Map;

public record GraphResponse(
        String  status,
        String  threadId,
        String  interruptPrompt,
        String  error,
        Map<String, Object> state
) {
    public static GraphResponse from(RunResult result) {
        return new GraphResponse(
                result.status().name(),
                result.threadId(),
                result.interruptPrompt(),
                result.error(),
                result.finalState().asMap()
        );
    }
}
