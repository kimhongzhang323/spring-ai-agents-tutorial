package com.masterclass.lc4jagentic;

public record WorkflowResult(
        String topic,
        ResearchResult research,
        CritiqueResult critique,
        String finalReport
) {}
