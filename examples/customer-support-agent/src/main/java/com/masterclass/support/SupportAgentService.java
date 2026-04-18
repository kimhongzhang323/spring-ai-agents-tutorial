package com.masterclass.support;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * Capstone agent combining tool-calling + RAG + per-session memory + guardrails.
 * Composing modules 04 + 05 + 06 + 09 into one production-grade support bot.
 */
@Service
public class SupportAgentService {

    private static final String SYSTEM_PROMPT = """
            You are a helpful customer support agent for Acme Corp.
            You have access to tools for looking up orders, creating support tickets, and processing refunds.
            You also have access to our product FAQ and documentation via the knowledge base.

            Guidelines:
            - Always greet the customer by name if you know it.
            - Check the order status tool before making claims about delivery dates.
            - Create a ticket only when the issue cannot be resolved in conversation.
            - For refunds, always confirm intent before calling the refund tool.
            - Be empathetic but concise.
            """;

    private final ChatClient chatClient;

    public SupportAgentService(ChatClient.Builder builder, VectorStore vectorStore,
                                SupportTools tools) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(new InMemoryChatMemory()),
                        new QuestionAnswerAdvisor(vectorStore,
                                SearchRequest.builder().topK(3).similarityThreshold(0.6).build()),
                        new SimpleLoggerAdvisor()
                )
                .defaultTools(tools)
                .build();
    }

    public String chat(String conversationId, String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY,
                        conversationId))
                .call()
                .content();
    }
}
