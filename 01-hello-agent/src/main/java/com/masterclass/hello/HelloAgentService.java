package com.masterclass.hello;

import com.masterclass.shared.guardrails.InputValidator;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class HelloAgentService {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant in the Java AI Agents Masterclass.
            Be concise. If asked something you cannot answer safely, say so clearly.
            """;

    private final ChatClient chatClient;
    private final InputValidator inputValidator;

    public HelloAgentService(ChatClient.Builder chatClientBuilder, InputValidator inputValidator) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
        this.inputValidator = inputValidator;
    }

    @Retry(name = "llmRetry", fallbackMethod = "fallbackResponse")
    public String chat(String userMessage) {
        var validation = inputValidator.validate(userMessage);
        if (!validation.valid()) {
            throw new IllegalArgumentException(validation.reason());
        }

        return chatClient
                .prompt()
                .user(userMessage)
                .call()
                .content();
    }

    // Called by Resilience4j if all retries are exhausted
    public String fallbackResponse(String userMessage, Exception ex) {
        return "I'm having trouble connecting to the AI model right now. Please try again in a moment.";
    }
}
