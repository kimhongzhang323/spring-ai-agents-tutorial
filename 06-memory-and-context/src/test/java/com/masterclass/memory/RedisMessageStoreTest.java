package com.masterclass.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisMessageStore — verifying memory behaviour WITHOUT a real Redis instance.
 *
 * We mock StringRedisTemplate so tests run fast and without infrastructure.
 * The real integration (sliding window, TTL, concurrent access) should be verified
 * with Testcontainers in RedisMessageStoreIT.java.
 *
 * Key behaviours tested here:
 * 1. Messages are stored and retrieved correctly
 * 2. Sliding window eviction (maxMessages cap)
 * 3. Clear removes the Redis key
 * 4. Graceful handling when Redis returns null (empty history)
 * 5. lastN param returns only the tail of the message list
 */
@ExtendWith(MockitoExtension.class)
class RedisMessageStoreTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    ObjectMapper objectMapper = new ObjectMapper();
    MemoryProperties props = new MemoryProperties(5, 60); // maxMessages=5

    RedisMessageStore store;

    @BeforeEach
    void setup() {
        when(redis.opsForValue()).thenReturn(valueOps);
        store = new RedisMessageStore(redis, objectMapper, props);
    }

    @Test
    void addAndGetRoundTrip() throws Exception {
        List<Message> messages = List.of(
                new UserMessage("Hello"),
                new AssistantMessage("Hi there!")
        );

        // Simulate Redis returning null first (empty store)
        when(valueOps.get(any())).thenReturn(null);
        store.add("conv-1", messages);

        // Capture what was stored and return it on get
        var jsonCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(any(), jsonCaptor.capture(), any());

        when(valueOps.get(any())).thenReturn(jsonCaptor.getValue());
        List<Message> retrieved = store.get("conv-1", Integer.MAX_VALUE);

        assertThat(retrieved).hasSize(2);
        assertThat(retrieved.get(0).getText()).isEqualTo("Hello");
        assertThat(retrieved.get(1).getText()).isEqualTo("Hi there!");
    }

    @Test
    void slidingWindowEvictsOldestMessages() throws Exception {
        // Pre-populate with 4 existing messages (maxMessages=5)
        List<Message> existing = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            existing.add(new UserMessage("msg-" + i));
        }
        when(valueOps.get(any())).thenReturn(objectMapper.writeValueAsString(existing));

        // Add 2 more messages — total would be 6, should be capped to 5
        store.add("conv-1", List.of(new UserMessage("msg-5"), new UserMessage("msg-6")));

        var jsonCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(any(), jsonCaptor.capture(), any());

        // Deserialize what was actually stored
        var storedType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, Message.class);
        // Verify the stored JSON has 5 items (not 6)
        var stored = objectMapper.readValue(jsonCaptor.getValue(),
                new com.fasterxml.jackson.core.type.TypeReference<List<java.util.LinkedHashMap<String, Object>>>() {});
        assertThat(stored).hasSize(5);
    }

    @Test
    void getReturnsEmptyListWhenRedisReturnsNull() {
        when(valueOps.get(any())).thenReturn(null);

        List<Message> result = store.get("non-existent", Integer.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    @Test
    void clearDeletesRedisKey() {
        store.clear("conv-1");

        verify(redis).delete("chat:memory:conv-1");
    }

    @Test
    void getLastNReturnsOnlyTailMessages() throws Exception {
        List<Message> messages = List.of(
                new UserMessage("old-1"),
                new UserMessage("old-2"),
                new UserMessage("recent-1"),
                new UserMessage("recent-2")
        );
        when(valueOps.get(any())).thenReturn(objectMapper.writeValueAsString(messages));

        List<Message> lastTwo = store.get("conv-1", 2);

        assertThat(lastTwo).hasSize(2);
        assertThat(lastTwo.get(0).getText()).isEqualTo("recent-1");
        assertThat(lastTwo.get(1).getText()).isEqualTo("recent-2");
    }

    @Test
    void addHandlesRedisFailureGracefully() {
        when(valueOps.get(any())).thenThrow(new RuntimeException("Redis connection refused"));

        // Should not throw — fails silently with a warning log
        store.add("conv-1", List.of(new UserMessage("hello")));
    }

    @Test
    void conversationScopingUsesCorrectRedisKey() {
        when(valueOps.get("chat:memory:alice:conv-1")).thenReturn(null);

        store.get("alice:conv-1", Integer.MAX_VALUE);

        verify(valueOps).get("chat:memory:alice:conv-1");
    }
}
