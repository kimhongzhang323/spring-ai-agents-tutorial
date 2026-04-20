package com.masterclass.parallelteam.agent;

import com.masterclass.parallelteam.event.AgentEvent;
import com.masterclass.parallelteam.event.AgentEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ResearchAgent {

    private static final Logger log = LoggerFactory.getLogger(ResearchAgent.class);

    private final ChatClient chatClient;
    private final AgentEventBus eventBus;

    public ResearchAgent(ChatClient.Builder chatClientBuilder, AgentEventBus eventBus) {
        this.chatClient = chatClientBuilder.build();
        this.eventBus = eventBus;
    }

    public String research(String jobId, String topic) {
        log.info("[job={}] ResearchAgent starting on topic: {}", jobId, topic);
        try {
            String facts = chatClient.prompt()
                    .system("""
                            You are a research specialist. Your sole task is to gather factual,
                            evidence-based information about the given topic.
                            Return 5–7 key facts as a concise bulleted list.
                            Do not editorialize; only state verifiable facts.
                            """)
                    .user("Research topic: " + topic)
                    .call()
                    .content();

            eventBus.publish(new AgentEvent.ResearchCompleted(jobId, facts, Instant.now()));
            log.info("[job={}] ResearchAgent completed", jobId);
            return facts;
        } catch (Exception e) {
            eventBus.publish(new AgentEvent.AgentFailed(jobId, "ResearchAgent", e.getMessage(), Instant.now()));
            throw e;
        }
    }
}
