package com.masterclass.microservices;

import com.masterclass.microservices.messaging.rabbitmq.AgentTaskConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentTaskConsumerTest {

    @Test
    void consumeTaskShouldAnalyzePayloadAndPublishResult() {
        var chatClientBuilder = mock(ChatClient.Builder.class);
        var chatClient = mock(ChatClient.class);
        var promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var callSpec = mock(ChatClient.CallResponseSpec.class);
        var rabbitTemplate = mock(RabbitTemplate.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(anyString())).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(
                "{\"taskType\":\"FULFILL_ORDER\",\"priority\":\"HIGH\",\"action\":\"SHIP\",\"reason\":\"High-value order\"}");

        var consumer = new AgentTaskConsumer(chatClientBuilder, rabbitTemplate);
        consumer.consumeTask("{\"task\":\"FULFILL_ORDER\",\"orderId\":\"ORD-001\"}");

        verify(rabbitTemplate).convertAndSend(
                eq("agent.exchange"),
                eq("agent.task.result"),
                (Object) argThat(msg -> msg.toString().contains("FULFILL_ORDER")));
    }

    @Test
    void consumeTaskShouldRethrowOnLlmFailureSoRabbitMqNacksAndDlqs() {
        var chatClientBuilder = mock(ChatClient.Builder.class);
        var chatClient = mock(ChatClient.class);
        var promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var callSpec = mock(ChatClient.CallResponseSpec.class);
        var rabbitTemplate = mock(RabbitTemplate.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(anyString())).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenThrow(new RuntimeException("LLM rate limit exceeded"));

        var consumer = new AgentTaskConsumer(chatClientBuilder, rabbitTemplate);

        assertThatThrownBy(() -> consumer.consumeTask("{\"task\":\"TEST\"}"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Agent task processing failed");

        verifyNoInteractions(rabbitTemplate);
    }
}
