package com.masterclass.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Token-efficient conversation memory using summarization.
 *
 * <h2>The token budget problem</h2>
 * A sliding-window memory (as in {@link ConversationService}) discards old messages entirely.
 * This loses context: if the user mentioned their name in message 1 and the window is 20 messages,
 * after 21 turns the agent forgets who it's talking to.
 *
 * <h2>Summarizing memory approach</h2>
 * Instead of discarding, compress: when the history exceeds {@code summarizeThreshold} messages,
 * summarize the oldest half into a single "memory summary" message that replaces them.
 * <pre>
 *   Before: [msg1, msg2, msg3, msg4, msg5, msg6, msg7, msg8] (8 messages, threshold=6)
 *   After:  [SUMMARY("user is Alice, works at Acme, asked about billing"), msg7, msg8]
 * </pre>
 *
 * <h2>Memory strategies comparison</h2>
 * <table>
 *   <tr><th>Strategy</th><th>Token cost</th><th>Context retention</th><th>Implementation</th></tr>
 *   <tr><td>No memory</td><td>Lowest</td><td>None</td><td>No advisor</td></tr>
 *   <tr><td>Full history</td><td>Grows unbounded</td><td>Perfect</td><td>InMemoryChatMemory</td></tr>
 *   <tr><td>Sliding window</td><td>Fixed</td><td>Recent only</td><td>RedisMessageStore (this repo)</td></tr>
 *   <tr><td>Summarizing</td><td>Grows slowly</td><td>Gist of everything</td><td>This service</td></tr>
 *   <tr><td>Entity extraction</td><td>Fixed</td><td>Key facts only</td><td>Custom advisor (advanced)</td></tr>
 * </table>
 *
 * <h2>When to use summarizing memory</h2>
 * Use it for long-running customer support sessions or coaching chatbots where the early
 * context (user's name, stated goals, prior issues) is important but the exact wording is not.
 */
@Service
public class SummarizingMemoryService {

    private static final Logger log = LoggerFactory.getLogger(SummarizingMemoryService.class);

    private static final int SUMMARIZE_THRESHOLD = 10;
    private static final String SUMMARY_ROLE_MARKER = "[MEMORY SUMMARY] ";

    private final ChatClient chatClient;
    private final RedisMessageStore messageStore;

    public SummarizingMemoryService(ChatClient.Builder builder, RedisMessageStore messageStore) {
        this.messageStore = messageStore;
        this.chatClient = builder
                .defaultSystem("""
                        You are a helpful assistant with good memory.
                        When a [MEMORY SUMMARY] is present in the conversation, treat it as your
                        long-term memory of the user's context. Refer to it naturally.
                        """)
                .build();
    }

    public ConversationService.ConversationTurn chat(String conversationId, String userId, String message) {
        String scopedId = userId + ":" + conversationId;

        // Load current history
        List<Message> history = new ArrayList<>(messageStore.get(scopedId, Integer.MAX_VALUE));

        // Compress if over threshold
        if (history.size() >= SUMMARIZE_THRESHOLD) {
            history = compress(history, scopedId);
        }

        // Build the prompt manually with history + new message
        String contextualPrompt = buildPromptWithHistory(history, message);
        String reply = chatClient.prompt()
                .user(contextualPrompt)
                .call()
                .content();

        // Persist new turn
        messageStore.add(scopedId, List.of(
                new UserMessage(message),
                new AssistantMessage(reply)
        ));

        log.debug("Summarizing memory: {} messages in store for {}", history.size() + 2, scopedId);
        return new ConversationService.ConversationTurn(conversationId, message, reply);
    }

    /**
     * Compresses old messages by summarizing the first half into a memory summary.
     * The summary is prefixed with {@link #SUMMARY_ROLE_MARKER} so the system prompt
     * can refer to it distinctly from the actual conversation history.
     */
    private List<Message> compress(List<Message> history, String scopedId) {
        int splitPoint = history.size() / 2;
        List<Message> toSummarize = history.subList(0, splitPoint);
        List<Message> toKeep = history.subList(splitPoint, history.size());

        String conversationText = toSummarize.stream()
                .map(m -> m.getMessageType().name() + ": " + m.getText())
                .reduce("", (a, b) -> a + "\n" + b);

        String summary = chatClient.prompt()
                .user("""
                        Summarize the following conversation excerpt into a compact memory note.
                        Extract: user's name/role if mentioned, stated goals, key facts discussed, any preferences.
                        Format: bullet points, max 5 bullets, very concise.

                        Conversation:
                        """ + conversationText)
                .call()
                .content();

        log.info("Compressed {} messages into summary for {}", toSummarize.size(), scopedId);

        // Replace old messages with the summary as a user message (visible to LLM as context)
        List<Message> compressed = new ArrayList<>();
        compressed.add(new UserMessage(SUMMARY_ROLE_MARKER + summary));
        compressed.addAll(toKeep);

        // Update the store with compressed history
        messageStore.clear(scopedId);
        messageStore.add(scopedId, compressed);

        return compressed;
    }

    private String buildPromptWithHistory(List<Message> history, String newMessage) {
        if (history.isEmpty()) {
            return newMessage;
        }
        var sb = new StringBuilder();
        sb.append("Previous conversation:\n");
        for (Message msg : history) {
            String role = switch (msg.getMessageType()) {
                case USER -> "User";
                case ASSISTANT -> "Assistant";
                default -> msg.getMessageType().name();
            };
            sb.append(role).append(": ").append(msg.getText()).append("\n");
        }
        sb.append("\nUser: ").append(newMessage);
        return sb.toString();
    }
}
