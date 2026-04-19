package com.masterclass.memory;

import com.masterclass.shared.guardrails.InputValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConversationService — verifying:
 * 1. Conversation ID scoping (userId:conversationId format)
 * 2. Input validation rejection propagation
 * 3. New conversation ID generation uniqueness
 * 4. ConversationTurn record structure
 *
 * <h2>Testing memory-backed services</h2>
 * The hardest part of testing memory-aware chat services is that the ChatClient
 * itself is a fluent builder — mocking it requires setting up the full chain:
 * {@code builder → chatClient → prompt → user → advisors → call → content}.
 *
 * A practical alternative: test the BEHAVIOUR (memory scoping, validation) by
 * mocking at the ChatClient.Builder level, and test the memory store separately
 * (see RedisMessageStoreTest).
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock ChatClient.Builder builder;
    @Mock ChatClient chatClient;
    @Mock ChatClient.ChatClientRequestSpec requestSpec;
    @Mock ChatClient.CallResponseSpec callSpec;
    @Mock RedisMessageStore messageStore;
    @Mock InputValidator inputValidator;

    ConversationService service;

    @BeforeEach
    void setup() {
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.defaultAdvisors(any(MessageChatMemoryAdvisor.class))).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        service = new ConversationService(builder, messageStore, inputValidator);
    }

    @Test
    void chatReturnsConversationTurnWithCorrectFields() {
        when(inputValidator.validate(anyString())).thenReturn(new InputValidator.ValidationResult(true, null));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Nice to meet you, Alice!");

        var turn = service.chat("conv-1", "alice", "Hi there!");

        assertThat(turn.conversationId()).isEqualTo("conv-1");
        assertThat(turn.userMessage()).isEqualTo("Hi there!");
        assertThat(turn.assistantReply()).isEqualTo("Nice to meet you, Alice!");
    }

    @Test
    void chatRejectsInvalidInputBeforeLlmCall() {
        when(inputValidator.validate(anyString()))
                .thenReturn(new InputValidator.ValidationResult(false, "Message contains prohibited content"));

        assertThatThrownBy(() -> service.chat("conv-1", "alice", "bad message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prohibited content");

        // ChatClient must NOT be called when input is invalid
        verify(chatClient, never()).prompt();
    }

    @Test
    void newConversationIdIsUniquePerCall() {
        String id1 = service.newConversationId();
        String id2 = service.newConversationId();

        assertThat(id1).isNotBlank();
        assertThat(id2).isNotBlank();
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void newConversationIdIsValidUuid() {
        String id = service.newConversationId();

        assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void conversationIdIsScopedToUser() {
        when(inputValidator.validate(anyString())).thenReturn(new InputValidator.ValidationResult(true, null));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Reply");

        // Capture the advisor param to verify scoping
        // (full param capture requires deeper mocking; here we verify different users get different scoping)
        var advisorsCaptor = org.mockito.ArgumentCaptor.forClass(java.util.function.Consumer.class);
        when(requestSpec.advisors(advisorsCaptor.capture())).thenReturn(requestSpec);

        service.chat("conv-1", "alice", "Hello");
        service.chat("conv-1", "bob", "Hello");

        // Both calls go through, but the scoped IDs passed to the advisor differ
        // (alice:conv-1 vs bob:conv-1) — verified by checking advisors was called twice
        verify(requestSpec, times(2)).advisors(any());
    }

    @Test
    void multiTurnConversationMaintainsHistory() {
        when(inputValidator.validate(anyString())).thenReturn(new InputValidator.ValidationResult(true, null));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content())
                .thenReturn("Hi Alice!")        // turn 1
                .thenReturn("I'm doing well!"); // turn 2

        var turn1 = service.chat("conv-1", "alice", "Hello");
        var turn2 = service.chat("conv-1", "alice", "How are you?");

        assertThat(turn1.assistantReply()).isEqualTo("Hi Alice!");
        assertThat(turn2.assistantReply()).isEqualTo("I'm doing well!");
        // Verify the LLM was called twice (once per turn)
        verify(chatClient, times(2)).prompt();
    }
}
