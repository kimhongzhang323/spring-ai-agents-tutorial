package com.masterclass.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis-backed ChatMemory implementation.
 *
 * Why Redis instead of InMemoryChatMemory:
 * - Survives application restarts.
 * - Works correctly under horizontal scaling (multiple app instances share the same Redis).
 * - TTL-based eviction prevents unbounded memory growth.
 *
 * InMemoryChatMemory is fine for demos and single-instance deployments.
 * Use this implementation (or a database-backed equivalent) in production.
 */
@Component
public class RedisMessageStore implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageStore.class);
    private static final String KEY_PREFIX = "chat:memory:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final MemoryProperties props;

    public RedisMessageStore(StringRedisTemplate redis, ObjectMapper objectMapper, MemoryProperties props) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        try {
            List<Message> existing = getOrEmpty(key);
            existing.addAll(messages);

            // Apply sliding window — keep only the last maxMessages
            if (existing.size() > props.maxMessages()) {
                existing = existing.subList(existing.size() - props.maxMessages(), existing.size());
            }

            List<StoredMessage> toStore = existing.stream().map(StoredMessage::from).toList();
            redis.opsForValue().set(key, objectMapper.writeValueAsString(toStore),
                    Duration.ofMinutes(props.ttlMinutes()));
        } catch (Exception e) {
            log.warn("Failed to persist messages for conversation {}: {}", conversationId, e.getMessage());
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        return get(conversationId, props.maxMessages());
    }

    public List<Message> get(String conversationId, int lastN) {
        List<Message> all = getOrEmpty(KEY_PREFIX + conversationId);
        if (lastN >= all.size()) return all;
        return new ArrayList<>(all.subList(all.size() - lastN, all.size()));
    }

    @Override
    public void clear(String conversationId) {
        redis.delete(KEY_PREFIX + conversationId);
    }

    private List<Message> getOrEmpty(String key) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return new ArrayList<>();
            List<StoredMessage> stored = objectMapper.readValue(json, new TypeReference<List<StoredMessage>>() {});
            return stored.stream().map(StoredMessage::toMessage).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } catch (Exception e) {
            log.warn("Failed to deserialize messages for key {}: {}", key, e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── Serialization DTO — avoids Jackson polymorphic issues with the Message interface ───────

    record StoredMessage(String type, String text) {
        static StoredMessage from(Message msg) {
            return new StoredMessage(msg.getMessageType().name(), msg.getText());
        }

        Message toMessage() {
            return switch (type) {
                case "ASSISTANT" -> new AssistantMessage(text);
                case "SYSTEM"    -> new SystemMessage(text);
                default          -> new UserMessage(text);
            };
        }
    }
}
