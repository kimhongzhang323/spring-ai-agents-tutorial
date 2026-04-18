package com.masterclass.lc4jagentic;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the Research → Critic → Synthesizer pipeline.
 *
 * Pattern: sequential workflow with a critic loop.
 * The critic can flag NEEDS_REVISION; in production you would re-invoke
 * ResearchAgent with the critique as additional context. Here we proceed
 * regardless after one pass to keep the demo deterministic.
 *
 * LLM calls: 3 total (research, critique, synthesize) — compare to Spring AI
 * supervisor which uses 4+ calls (1 routing + 3 sub-agents).
 */
@Service
public class ResearchWorkflow {

    private static final Logger log = LoggerFactory.getLogger(ResearchWorkflow.class);

    private final ResearchAgent researcher;
    private final CriticAgent critic;
    private final SynthesizerAgent synthesizer;
    private final MeterRegistry meters;

    public ResearchWorkflow(ResearchAgent researcher, CriticAgent critic,
                            SynthesizerAgent synthesizer, MeterRegistry meters) {
        this.researcher  = researcher;
        this.critic      = critic;
        this.synthesizer = synthesizer;
        this.meters      = meters;
    }

    public WorkflowResult run(String topic) {
        log.debug("Starting research workflow for topic: {}", topic);

        // Step 1: Research
        meters.counter("workflow.step", "step", "research").increment();
        ResearchResult research = researcher.research(topic);
        log.debug("Research complete. Key findings length: {}", research.keyFindings().length());

        // Step 2: Critique
        meters.counter("workflow.step", "step", "critique").increment();
        String researchText = formatResearch(research);
        CritiqueResult critique = critic.critique(topic, researchText);
        log.debug("Critique verdict: {}", critique.verdict());

        if (!critique.approved()) {
            log.info("Research flagged as NEEDS_REVISION — proceeding with critique in context");
            meters.counter("workflow.revision_requested").increment();
        }

        // Step 3: Synthesize
        meters.counter("workflow.step", "step", "synthesize").increment();
        String report = synthesizer.synthesize(topic, researchText, critique.feedback());

        return new WorkflowResult(topic, research, critique, report);
    }

    private String formatResearch(ResearchResult r) {
        return "Summary: " + r.summary() + "\n\nKey Findings: " + r.keyFindings()
                + "\n\nLimitations: " + r.limitations();
    }
}
