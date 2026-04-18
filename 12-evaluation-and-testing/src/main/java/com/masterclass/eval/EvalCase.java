package com.masterclass.eval;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One row in a golden dataset YAML file.
 * question  — the user input sent to the agent under test
 * contexts  — ideal source chunks that should be retrieved (RAG eval)
 * expected  — the ground-truth answer used by the LLM judge
 */
public record EvalCase(
        String id,
        String question,
        List<String> contexts,
        @JsonProperty("expected_answer") String expectedAnswer
) {}
