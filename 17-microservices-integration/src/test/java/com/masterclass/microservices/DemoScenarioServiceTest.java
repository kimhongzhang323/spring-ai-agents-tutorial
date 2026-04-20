package com.masterclass.microservices;

import com.masterclass.microservices.cache.redis.RedisSemanticCacheTool;
import com.masterclass.microservices.databases.elasticsearch.ElasticsearchTool;
import com.masterclass.microservices.databases.mongodb.MongoTool;
import com.masterclass.microservices.databases.postgres.PostgresTool;
import com.masterclass.microservices.messaging.kafka.KafkaTool;
import com.masterclass.microservices.messaging.rabbitmq.RabbitMqTool;
import com.masterclass.microservices.messaging.redis.RedisStreamsTool;
import com.masterclass.microservices.orchestrator.DemoResult;
import com.masterclass.microservices.orchestrator.DemoScenarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DemoScenarioServiceTest {

    private DemoScenarioService service;

    private KafkaTool kafkaTool;
    private RabbitMqTool rabbitMqTool;
    private RedisStreamsTool redisStreamsTool;
    private RedisSemanticCacheTool redisCache;
    private PostgresTool postgresTool;
    private MongoTool mongoTool;
    private ElasticsearchTool elasticsearchTool;

    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec promptSpec;
    private ChatClient.CallResponseSpec callSpec;

    @BeforeEach
    void setUp() {
        kafkaTool = mock(KafkaTool.class);
        rabbitMqTool = mock(RabbitMqTool.class);
        redisStreamsTool = mock(RedisStreamsTool.class);
        redisCache = mock(RedisSemanticCacheTool.class);
        postgresTool = mock(PostgresTool.class);
        mongoTool = mock(MongoTool.class);
        elasticsearchTool = mock(ElasticsearchTool.class);

        chatClientBuilder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(anyString())).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(
                "{\"priority\":\"HIGH\",\"risk\":\"LOW\",\"action\":\"APPROVE\"}");

        service = new DemoScenarioService(
                chatClientBuilder, kafkaTool, rabbitMqTool, redisStreamsTool,
                redisCache, postgresTool, mongoTool, elasticsearchTool);
    }

    // ── orderPipeline ──────────────────────────────────────────────────────────

    @Test
    void orderPipelineShouldPublishToKafkaFirst() {
        when(kafkaTool.publishToKafka(anyString(), anyString())).thenReturn("partition=0 offset=42");
        when(redisCache.getCachedResponse(anyString())).thenReturn("CACHE_MISS");
        when(redisCache.cacheResponse(anyString(), anyString())).thenReturn("cached");
        when(redisStreamsTool.appendToRedisStream(anyString(), anyString())).thenReturn("stream-id-1");
        when(rabbitMqTool.publishToRabbitMq(anyString())).thenReturn("published to RabbitMQ");
        when(postgresTool.getPostgresSchema()).thenReturn("[{\"table\":\"orders\"}]");

        DemoResult result = service.orderPipeline("ORD-001", "{\"total\":100}");

        assertThat(result.scenario()).isEqualTo("order-pipeline");
        assertThat(result.steps()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(result.steps().get(0).tool()).isEqualTo("kafka");
        assertThat(result.steps().get(0).success()).isTrue();
        verify(kafkaTool).publishToKafka(eq("ORDER_RECEIVED"), contains("ORD-001"));
    }

    @Test
    void orderPipelineShouldSkipLlmOnCacheHit() {
        when(kafkaTool.publishToKafka(anyString(), anyString())).thenReturn("ok");
        when(redisCache.getCachedResponse(anyString()))
                .thenReturn("{\"priority\":\"HIGH\",\"risk\":\"LOW\",\"action\":\"APPROVE\"}");
        when(redisStreamsTool.appendToRedisStream(anyString(), anyString())).thenReturn("ok");
        when(rabbitMqTool.publishToRabbitMq(anyString())).thenReturn("ok");
        when(postgresTool.getPostgresSchema()).thenReturn("[]");

        DemoResult result = service.orderPipeline("ORD-002", "{\"total\":50}");

        // LLM should NOT be called because cache returned a hit
        verify(chatClient, never()).prompt();
        assertThat(result.steps().stream().anyMatch(s -> s.tool().equals("redis-cache") && s.result().contains("HIT")))
                .isTrue();
    }

    @Test
    void orderPipelineShouldReturnResultWithTimestamp() {
        when(kafkaTool.publishToKafka(anyString(), anyString())).thenReturn("ok");
        when(redisCache.getCachedResponse(anyString())).thenReturn("CACHE_MISS");
        when(redisCache.cacheResponse(anyString(), anyString())).thenReturn("ok");
        when(redisStreamsTool.appendToRedisStream(anyString(), anyString())).thenReturn("ok");
        when(rabbitMqTool.publishToRabbitMq(anyString())).thenReturn("ok");
        when(postgresTool.getPostgresSchema()).thenReturn("[]");

        DemoResult result = service.orderPipeline("ORD-003", "{}");

        assertThat(result.timestamp()).isNotNull();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    // ── cacheFirstPattern ─────────────────────────────────────────────────────

    @Test
    void cacheFirstShouldCallLlmOnMissAndReturnHitOnSecondCall() {
        when(redisCache.getCachedResponse(anyString()))
                .thenReturn("CACHE_MISS")
                .thenReturn("Kafka is a distributed event streaming platform.");
        when(redisCache.cacheResponse(anyString(), anyString())).thenReturn("cached");
        when(callSpec.content()).thenReturn("Kafka is a distributed event streaming platform.");

        DemoResult result = service.cacheFirstPattern("What is Kafka?");

        assertThat(result.scenario()).isEqualTo("cache-first");
        assertThat(result.steps()).hasSize(4);
        assertThat(result.steps().get(0).result()).isEqualTo("CACHE_MISS");
        assertThat(result.steps().get(3).result()).contains("Kafka");
        assertThat(result.summary()).contains("Round 1 called LLM");
    }

    @Test
    void cacheFirstShouldIndicateCacheUnavailableWhenRedisDown() {
        when(redisCache.getCachedResponse(anyString())).thenThrow(new RuntimeException("Redis timeout"));
        when(redisCache.cacheResponse(anyString(), anyString())).thenThrow(new RuntimeException("Redis timeout"));
        when(callSpec.content()).thenReturn("An answer from LLM.");

        // Should not throw — safeRun handles exceptions gracefully
        DemoResult result = service.cacheFirstPattern("What is Kafka?");

        assertThat(result.steps()).isNotEmpty();
        assertThat(result.steps().get(0).result()).contains("unavailable");
    }

    // ── multiBrokerFanout ─────────────────────────────────────────────────────

    @Test
    void multiBrokerFanoutShouldPublishToAllThreeBrokers() {
        when(kafkaTool.publishToKafka(anyString(), anyString())).thenReturn("kafka-ok");
        when(rabbitMqTool.publishToRabbitMq(anyString())).thenReturn("rabbit-ok");
        when(redisStreamsTool.appendToRedisStream(anyString(), anyString())).thenReturn("stream-ok");
        when(redisStreamsTool.readFromRedisStream()).thenReturn("[{\"id\":\"1\",\"value\":{}}]");

        DemoResult result = service.multiBrokerFanout("USER_SIGNUP", "{\"userId\":\"u1\"}");

        assertThat(result.scenario()).isEqualTo("multi-broker-fanout");
        assertThat(result.steps()).hasSize(4);
        assertThat(result.steps().get(0).tool()).isEqualTo("kafka");
        assertThat(result.steps().get(1).tool()).isEqualTo("rabbitmq");
        assertThat(result.steps().get(2).tool()).isEqualTo("redis-streams");
        assertThat(result.steps().get(3).action()).contains("readFromRedisStream");
        verify(kafkaTool).publishToKafka(eq("USER_SIGNUP"), contains("userId"));
        verify(rabbitMqTool).publishToRabbitMq(contains("USER_SIGNUP"));
        verify(redisStreamsTool).appendToRedisStream(eq("USER_SIGNUP"), eq("{\"userId\":\"u1\"}"));
    }

    @Test
    void multiBrokerFanoutShouldContinueIfOneBrokerFails() {
        when(kafkaTool.publishToKafka(anyString(), anyString())).thenThrow(new RuntimeException("Kafka down"));
        when(rabbitMqTool.publishToRabbitMq(anyString())).thenReturn("rabbit-ok");
        when(redisStreamsTool.appendToRedisStream(anyString(), anyString())).thenReturn("stream-ok");
        when(redisStreamsTool.readFromRedisStream()).thenReturn("[]");

        // Should not throw — partial failure is recorded, not propagated
        DemoResult result = service.multiBrokerFanout("TEST_EVENT", "{}");

        assertThat(result.steps()).hasSize(4);
        assertThat(result.steps().get(0).success()).isFalse(); // kafka failed
        assertThat(result.steps().get(1).success()).isTrue();  // rabbit ok
        assertThat(result.steps().get(2).success()).isTrue();  // redis ok
    }

    // ── polyglotPersistence ────────────────────────────────────────────────────

    @Test
    void polyglotPersistenceShouldIntrospectAllThreeStores() {
        when(postgresTool.getPostgresSchema()).thenReturn("[{\"table\":\"users\"}]");
        when(mongoTool.listMongoCollections()).thenReturn("[\"events\",\"sessions\"]");
        when(elasticsearchTool.searchElasticsearch(any(), any())).thenReturn("[{\"_id\":\"1\"}]");
        when(callSpec.content()).thenReturn("Use Elasticsearch for log queries.");

        DemoResult result = service.polyglotPersistence("Find recent error logs");

        assertThat(result.scenario()).isEqualTo("polyglot-persistence");
        assertThat(result.steps()).hasSize(4);
        assertThat(result.steps().get(0).tool()).isEqualTo("postgres");
        assertThat(result.steps().get(1).tool()).isEqualTo("mongodb");
        assertThat(result.steps().get(2).tool()).isEqualTo("elasticsearch");
        assertThat(result.steps().get(3).tool()).isEqualTo("llm");
        assertThat(result.summary()).contains("Elasticsearch");
    }

    @Test
    void polyglotPersistenceShouldReturnPartialResultIfOneStoreUnavailable() {
        when(postgresTool.getPostgresSchema()).thenThrow(new RuntimeException("DB connection refused"));
        when(mongoTool.listMongoCollections()).thenReturn("[\"logs\"]");
        when(elasticsearchTool.searchElasticsearch(any(), any())).thenReturn("[]");
        when(callSpec.content()).thenReturn("Use MongoDB for this query.");

        DemoResult result = service.polyglotPersistence("Find user sessions");

        assertThat(result.steps()).hasSize(4);
        assertThat(result.steps().get(0).result()).contains("unavailable");
        assertThat(result.steps().get(1).success()).isTrue();
    }
}
