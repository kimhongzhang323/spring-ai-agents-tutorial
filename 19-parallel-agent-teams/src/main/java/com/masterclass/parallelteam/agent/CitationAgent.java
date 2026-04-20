package com.masterclass.parallelteam.agent;

import com.masterclass.parallelteam.event.AgentEvent;
import com.masterclass.parallelteam.event.AgentEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CitationAgent {

    private static final Logger log = LoggerFactory.getLogger(CitationAgent.class);

    private final ChatClient chatClient;
    private final AgentEventBus eventBus;

    public CitationAgent(ChatClient.Builder chatClientBuilder, AgentEventBus eventBus) {
        this.chatClient = chatClientBuilder.build();
        this.eventBus = eventBus;
    }

    public String findCitations(String jobId, String topic) {
        log.info("[job={}] CitationAgent starting on topic: {}", jobId, topic);
        try {
            String citations = chatClient.prompt()
                    .system("""
                            You are a citation specialist. Identify authoritative sources, studies,
                            and publications relevant to the given topic. Format each citation with:
                            - Title, Author/Organization, Year, and a one-sentence summary of relevance.
                            Return 4–6 citations. Note: you are generating plausible citation examples
                            for educational purposes — always verify citations in production.
                            """)
                    .user("Find supporting citations for: " + topic)
                    .call()
                    .content();

            eventBus.publish(new AgentEvent.CitationCompleted(jobId, citations, Instant.now()));
            log.info("[job={}] CitationAgent completed", jobId);
            return citations;
        } catch (Exception e) {
            eventBus.publish(new AgentEvent.AgentFailed(jobId, "CitationAgent", e.getMessage(), Instant.now()));
            throw e;
        }
    }
}
