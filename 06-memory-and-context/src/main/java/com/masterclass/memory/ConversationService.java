package com.masterclass.memory;

import com.masterclass.shared.guardrails.InputValidator;
import com.masterclass.shared.observability.TokenUsageMetrics;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ConversationService {

    private final ChatClient chatClient;
    private final InputValidator inputValidator;
    private final TokenUsageMetrics tokenUsageMetrics;
    private final RedisMessageStore memory;

    public ConversationService(ChatClient.Builder builder, RedisMessageStore memory,
                               InputValidator inputValidator, TokenUsageMetrics tokenUsageMetrics) {
        this.inputValidator = inputValidator;
        this.tokenUsageMetrics = tokenUsageMetrics;
        this.memory = memory;
        this.chatClient = builder
                .defaultSystem("You are a helpful assistant. You remember the context of our conversation.")
                /*
                 * MessageChatMemoryAdvisor injects prior messages before every LLM call.
                 * It loads the conversation history from RedisMessageStore, prepends it to
                 * the prompt, then saves the new user+assistant messages back after the response.
                 *
                 * The CONVERSATION_ID is the key used to look up the right history in Redis.
                 */
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();
    }

    public ConversationTurn chat(String conversationId, String userId, String message) {
        var validation = inputValidator.validate(message);
        if (!validation.valid()) throw new IllegalArgumentException(validation.reason());

        // Scope conversation to user — prevents user A from reading user B's history
        String scopedId = userId + ":" + conversationId;

        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, scopedId))
                .call()
                .chatResponse();

        var usage = chatResponse.getMetadata().getUsage();
        if (usage != null) {
            tokenUsageMetrics.record(
                usage.getPromptTokens() != null ? usage.getPromptTokens() : 0,
                usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0
            );
        }

        return new ConversationTurn(conversationId, message,
                chatResponse.getResult().getOutput().getText());
    }

    public String newConversationId() {
        return UUID.randomUUID().toString();
    }

    public void clearConversation(String conversationId, String userId) {
        memory.clear(userId + ":" + conversationId);
    }

    public record ConversationTurn(String conversationId, String userMessage, String assistantReply) {}
}
