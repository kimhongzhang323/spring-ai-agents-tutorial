package com.masterclass.lc4jagentic;

/**
 * Structured output from ResearchAgent.
 * LangChain4j's AiServices automatically converts the LLM response to this record
 * using a generated JSON schema injected into the system prompt.
 */
public record ResearchResult(
        String summary,
        String keyFindings,
        String limitations
) {}
