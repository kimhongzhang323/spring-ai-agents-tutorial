package com.masterclass.multiagent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class WriterAgent {

    private final ChatClient chatClient;

    public WriterAgent(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        You are a professional writer. Your job is to synthesise information into
                        a clear, well-structured, and engaging final report.
                        Use headings, bullet points where appropriate, and a professional tone.
                        End with an executive summary of 2-3 sentences.
                        """)
                .build();
    }

    @Tool(description = """
            Write a well-structured final report by synthesising research and analysis.
            Use this as the last step after research and analysis are complete.
            Input: combined research findings and analysis as a string.
            Returns: polished, well-formatted report ready for the user.
            """)
    public String write(String researchAndAnalysis) {
        return chatClient.prompt()
                .user("Write a final report based on this research and analysis:\n\n" + researchAndAnalysis)
                .call().content();
    }
}
