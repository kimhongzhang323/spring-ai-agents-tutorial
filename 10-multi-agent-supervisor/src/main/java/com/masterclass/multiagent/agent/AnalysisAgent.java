package com.masterclass.multiagent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class AnalysisAgent {

    private final ChatClient chatClient;

    public AnalysisAgent(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        You are an analytical specialist. Your job is to analyse information critically.
                        Identify patterns, pros/cons, risks, and opportunities.
                        Return a structured analysis with: Strengths, Weaknesses, Opportunities, Risks (SWOR).
                        """)
                .build();
    }

    @Tool(description = """
            Analyse information or data and identify patterns, strengths, weaknesses, opportunities, and risks.
            Use this when you have raw information that needs critical evaluation or structured analysis.
            Input: the content or data to analyse as a string.
            Returns: structured SWOR analysis.
            """)
    public String analyse(String content) {
        return chatClient.prompt()
                .user("Perform a critical analysis of this content:\n\n" + content)
                .call().content();
    }
}
