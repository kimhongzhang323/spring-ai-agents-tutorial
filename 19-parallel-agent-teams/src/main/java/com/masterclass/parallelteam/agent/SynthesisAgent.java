package com.masterclass.parallelteam.agent;

import com.masterclass.parallelteam.event.AgentEvent;
import com.masterclass.parallelteam.event.AgentEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Runs after receiving upstream events from ResearchAgent, AnalysisAgent, and CitationAgent.
 * Combines their outputs into a structured report. Invoked by TeamCoordinator once all three
 * parallel agents complete.
 */
@Service
public class SynthesisAgent {

    private static final Logger log = LoggerFactory.getLogger(SynthesisAgent.class);

    private final ChatClient chatClient;
    private final AgentEventBus eventBus;

    public SynthesisAgent(ChatClient.Builder chatClientBuilder, AgentEventBus eventBus) {
        this.chatClient = chatClientBuilder.build();
        this.eventBus = eventBus;
    }

    public String synthesize(String jobId, String topic, String facts, String trends, String citations) {
        log.info("[job={}] SynthesisAgent starting report generation", jobId);
        try {
            String report = chatClient.prompt()
                    .system("""
                            You are a senior analyst writing an executive briefing.
                            Combine the provided facts, trends, and citations into a coherent,
                            well-structured report with: Executive Summary, Key Findings, Trend Analysis,
                            Supporting Evidence, and Conclusion sections.
                            Be concise, professional, and evidence-driven.
                            """)
                    .user("""
                            Topic: %s

                            RESEARCH FACTS:
                            %s

                            TREND ANALYSIS:
                            %s

                            CITATIONS:
                            %s
                            """.formatted(topic, facts, trends, citations))
                    .call()
                    .content();

            eventBus.publish(new AgentEvent.SynthesisCompleted(jobId, report, Instant.now()));
            log.info("[job={}] SynthesisAgent completed report", jobId);
            return report;
        } catch (Exception e) {
            eventBus.publish(new AgentEvent.AgentFailed(jobId, "SynthesisAgent", e.getMessage(), Instant.now()));
            throw e;
        }
    }
}
