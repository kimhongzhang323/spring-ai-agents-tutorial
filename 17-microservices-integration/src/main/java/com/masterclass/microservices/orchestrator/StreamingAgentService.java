package com.masterclass.microservices.orchestrator;

import com.masterclass.microservices.messaging.kafka.KafkaTool;
import com.masterclass.microservices.messaging.rabbitmq.RabbitMqTool;
import com.masterclass.microservices.messaging.redis.RedisStreamsTool;
import com.masterclass.microservices.databases.postgres.PostgresTool;
import com.masterclass.microservices.databases.mongodb.MongoTool;
import com.masterclass.microservices.databases.elasticsearch.ElasticsearchTool;
import com.masterclass.microservices.cache.redis.RedisSemanticCacheTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Streams agent responses token-by-token via Spring AI's reactive API.
 *
 * Tool calls (Kafka, RabbitMQ, DB queries) execute synchronously before the LLM
 * begins streaming its textual response — this is the standard pattern because
 * tool results must be available in the context before generation begins.
 */
@Service
public class StreamingAgentService {

    private static final Logger log = LoggerFactory.getLogger(StreamingAgentService.class);

    private static final String SYSTEM_PROMPT = """
            You are a microservices integration expert. Help the user understand and interact with
            the system's databases, message queues, and services.
            Give clear, concise answers. Explain which tools you used and why.
            """;

    private final ChatClient chatClient;
    private final KafkaTool kafkaTool;
    private final RabbitMqTool rabbitMqTool;
    private final RedisStreamsTool redisStreamsTool;
    private final PostgresTool postgresTool;
    private final MongoTool mongoTool;
    private final ElasticsearchTool elasticsearchTool;
    private final RedisSemanticCacheTool redisCache;

    public StreamingAgentService(
            ChatClient.Builder chatClientBuilder,
            KafkaTool kafkaTool, RabbitMqTool rabbitMqTool,
            RedisStreamsTool redisStreamsTool, PostgresTool postgresTool,
            MongoTool mongoTool, ElasticsearchTool elasticsearchTool,
            RedisSemanticCacheTool redisCache) {
        this.chatClient = chatClientBuilder.build();
        this.kafkaTool = kafkaTool;
        this.rabbitMqTool = rabbitMqTool;
        this.redisStreamsTool = redisStreamsTool;
        this.postgresTool = postgresTool;
        this.mongoTool = mongoTool;
        this.elasticsearchTool = elasticsearchTool;
        this.redisCache = redisCache;
    }

    public Flux<String> executeStreaming(String userMessage) {
        log.debug("StreamingAgent starting: messageLength={}", userMessage.length());
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .tools(
                        kafkaTool, rabbitMqTool, redisStreamsTool,
                        postgresTool, mongoTool, elasticsearchTool,
                        redisCache
                )
                .stream()
                .content()
                .doOnComplete(() -> log.debug("StreamingAgent completed stream"))
                .doOnError(e -> log.error("StreamingAgent stream error", e));
    }
}
