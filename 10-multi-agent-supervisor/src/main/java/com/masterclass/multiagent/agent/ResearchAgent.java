package com.masterclass.multiagent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Sub-agent: researches a topic and returns a structured summary.
 *
 * Architecture rule: sub-agents are @Component beans with @Tool-annotated methods.
 * The Supervisor registers them as tools on its own ChatClient.
 * Sub-agents NEVER call each other — all routing goes through the Supervisor.
 */
@Component
public class ResearchAgent {

    private final ChatClient chatClient;

    public ResearchAgent(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        You are a research specialist. Your job is to gather and summarize
                        information about a given topic. Be factual, concise, and cite key points.
                        Return a structured summary with: Overview, Key Facts, and Open Questions.
                        """)
                .build();
    }

    @Tool(description = """
            Research a topic in depth and return a structured summary.
            Use this when you need factual background information, key facts, or an overview of a subject.
            Input: the topic to research as a string.
            Returns: structured summary with Overview, Key Facts, and Open Questions sections.
            """)
    public String research(String topic) {
        return chatClient.prompt()
                .user("Research this topic thoroughly: " + topic)
                .call().content();
    }
}
