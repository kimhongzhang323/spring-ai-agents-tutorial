package com.masterclass.lc4jagentic;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Evaluates research output and returns structured critique with an approval decision.
 * Used as a quality gate before synthesis — demonstrates the critic loop pattern.
 */
public interface CriticAgent {

    @SystemMessage("""
            You are a critical peer reviewer. Evaluate research output for:
            1. Factual accuracy (any obvious errors?)
            2. Completeness (missing important angles?)
            3. Clarity (is it well-explained?)
            Return a concise critique and an approval: APPROVED or NEEDS_REVISION.
            """)
    @UserMessage("""
            Topic: {{topic}}

            Research to review:
            {{research}}
            """)
    CritiqueResult critique(@V("topic") String topic, @V("research") String research);
}
