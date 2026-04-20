package com.masterclass.microservices;

import com.masterclass.microservices.cache.redis.RedisSemanticCacheTool;
import com.masterclass.microservices.databases.elasticsearch.ElasticsearchTool;
import com.masterclass.microservices.databases.mongodb.MongoTool;
import com.masterclass.microservices.databases.postgres.PostgresTool;
import com.masterclass.microservices.messaging.kafka.KafkaTool;
import com.masterclass.microservices.messaging.rabbitmq.RabbitMqTool;
import com.masterclass.microservices.messaging.redis.RedisStreamsTool;
import com.masterclass.microservices.orchestrator.StreamingAgentService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StreamingAgentServiceTest {

    @Test
    void executeStreamingShouldEmitTokensAndComplete() {
        var chatClientBuilder = mock(ChatClient.Builder.class);
        var chatClient = mock(ChatClient.class);
        var promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var streamSpec = mock(ChatClient.StreamResponseSpec.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(anyString())).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.tools(any(Object[].class))).thenReturn(promptSpec);
        when(promptSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.just("Hello", " world", "!"));

        var service = new StreamingAgentService(
                chatClientBuilder,
                mock(KafkaTool.class), mock(RabbitMqTool.class),
                mock(RedisStreamsTool.class), mock(PostgresTool.class),
                mock(MongoTool.class), mock(ElasticsearchTool.class),
                mock(RedisSemanticCacheTool.class));

        Flux<String> stream = service.executeStreaming("What is Kafka?");

        StepVerifier.create(stream)
                .expectNext("Hello", " world", "!")
                .verifyComplete();
    }

    @Test
    void executeStreamingShouldPropagateErrorFromLlm() {
        var chatClientBuilder = mock(ChatClient.Builder.class);
        var chatClient = mock(ChatClient.class);
        var promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var streamSpec = mock(ChatClient.StreamResponseSpec.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(anyString())).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.tools(any(Object[].class))).thenReturn(promptSpec);
        when(promptSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.error(new RuntimeException("LLM quota exceeded")));

        var service = new StreamingAgentService(
                chatClientBuilder,
                mock(KafkaTool.class), mock(RabbitMqTool.class),
                mock(RedisStreamsTool.class), mock(PostgresTool.class),
                mock(MongoTool.class), mock(ElasticsearchTool.class),
                mock(RedisSemanticCacheTool.class));

        Flux<String> stream = service.executeStreaming("Tell me about DynamoDB");

        StepVerifier.create(stream)
                .expectError(RuntimeException.class)
                .verify();
    }
}
