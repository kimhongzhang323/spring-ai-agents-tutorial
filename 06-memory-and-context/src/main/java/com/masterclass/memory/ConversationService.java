package com.masterclass.memory;

import com.masterclass.shared.guardrails.InputValidator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ConversationService {

    private final ChatClient chatClient;
    private final InputValidator inputValidator;

    public ConversationService(ChatClient.Builder builder, RedisMessageStore memory,
                               InputValidator inputValidator) {
        this.inputValidator = inputValidator;
        this.chatClient = builder
                .defaultSystem("You are a helpful assistant. You remember the context of our conversation.")
                /*
                 * MessageChatMemoryAdvisor injects prior messages before every LLM call.
                 * It loads the conversation history from RedisMessageStore, prepends it to
                 * the prompt, then saves the new user+assistant messages back after the response.
                 *
                 * The CONVERSATION_ID is the key used to look up the right history in Redis.
                 */
                .defaultAdvisors(new MessageChatMemoryAdvisor(memory))
                .build();
    }

    public ConversationTurn chat(String conversationId, String userId, String message) {
        var validation = inputValidator.validate(message);
        if (!validation.valid()) throw new IllegalArgumentException(validation.reason());

        // Scope conversation to user — prevents user A from reading user B's history
        String scopedId = userId + ":" + conversationId;

        String reply = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(MessageChatMemoryAdvisor.CONVERSATION_ID_KEY, scopedId))
                .call()
                .content();

        return new ConversationTurn(conversationId, message, reply);
    }

    public String newConversationId() {
        return UUID.randomUUID().toString();
    }

    public record ConversationTurn(String conversationId, String userMessage, String assistantReply) {}
}
