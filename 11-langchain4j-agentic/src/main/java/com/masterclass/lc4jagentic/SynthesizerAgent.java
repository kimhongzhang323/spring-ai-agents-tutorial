package com.masterclass.lc4jagentic;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Final step: produces a polished, reader-friendly report from research + critique.
 */
public interface SynthesizerAgent {

    @SystemMessage("""
            You are a professional technical writer. Using the provided research and
            reviewer feedback, write a clear, engaging report. Structure it as:
            - Executive Summary (2–3 sentences)
            - Main Findings (bullet points)
            - Critical Assessment
            - Conclusion
            Tone: informative, neutral, suitable for a senior engineering audience.
            """)
    @UserMessage("""
            Topic: {{topic}}

            Research findings:
            {{research}}

            Reviewer feedback:
            {{critique}}
            """)
    String synthesize(
            @V("topic") String topic,
            @V("research") String research,
            @V("critique") String critique
    );
}
