package com.masterclass.parallelteam.agent;

import com.masterclass.parallelteam.event.AgentEvent;
import com.masterclass.parallelteam.event.AgentEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AnalysisAgent {

    private static final Logger log = LoggerFactory.getLogger(AnalysisAgent.class);

    private final ChatClient chatClient;
    private final AgentEventBus eventBus;

    public AnalysisAgent(ChatClient.Builder chatClientBuilder, AgentEventBus eventBus) {
        this.chatClient = chatClientBuilder.build();
        this.eventBus = eventBus;
    }

    public String analyze(String jobId, String topic) {
        log.info("[job={}] AnalysisAgent starting on topic: {}", jobId, topic);
        try {
            String trends = chatClient.prompt()
                    .system("""
                            You are a trend analyst. Identify emerging patterns, risks, and opportunities
                            related to the given topic. Return 3–5 trend observations with a brief
                            impact assessment for each. Focus on what is changing and why it matters.
                            """)
                    .user("Analyze trends for: " + topic)
                    .call()
                    .content();

            eventBus.publish(new AgentEvent.AnalysisCompleted(jobId, trends, Instant.now()));
            log.info("[job={}] AnalysisAgent completed", jobId);
            return trends;
        } catch (Exception e) {
            eventBus.publish(new AgentEvent.AgentFailed(jobId, "AnalysisAgent", e.getMessage(), Instant.now()));
            throw e;
        }
    }
}
