package com.masterclass.hello;

import com.masterclass.shared.guardrails.InputValidator;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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

    /**
     * Blocking call — waits for the full response before returning.
     * Use for short responses or when downstream needs the complete text.
     */
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

    /**
     * Streaming call — returns a Flux<String> of response tokens as they arrive.
     *
     * Spring AI's .stream().content() uses the provider's streaming API (SSE from OpenAI,
     * streaming from Ollama) and emits each partial token as a separate Flux item.
     *
     * The controller exposes this as MediaType.TEXT_EVENT_STREAM_VALUE so the browser
     * receives a continuous stream rather than one large payload.
     *
     * Note: Resilience4j @Retry does not work with reactive streams — handle retries
     * at the Flux level using .retry() or .onErrorResume() if needed.
     */
    public Flux<String> stream(String userMessage) {
        var validation = inputValidator.validate(userMessage);
        if (!validation.valid()) {
            return Flux.just("Error: " + validation.reason());
        }

        return chatClient
                .prompt()
                .user(userMessage)
                .stream()
                .content();
    }

    public String fallbackResponse(String userMessage, Exception ex) {
        return "I'm having trouble connecting to the AI model right now. Please try again in a moment.";
    }
}
