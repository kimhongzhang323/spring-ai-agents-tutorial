package com.masterclass.microservices.orchestrator;

import com.masterclass.microservices.cache.hazelcast.HazelcastTool;
import com.masterclass.microservices.cache.redis.RedisSemanticCacheTool;
import com.masterclass.microservices.databases.cassandra.CassandraTool;
import com.masterclass.microservices.databases.clickhouse.ClickHouseTool;
import com.masterclass.microservices.databases.dynamodb.DynamoDbTool;
import com.masterclass.microservices.databases.elasticsearch.ElasticsearchTool;
import com.masterclass.microservices.databases.mongodb.MongoTool;
import com.masterclass.microservices.databases.neo4j.Neo4jTool;
import com.masterclass.microservices.databases.postgres.PostgresTool;
import com.masterclass.microservices.enterprise.camel.CamelRouteTool;
import com.masterclass.microservices.enterprise.cloudevents.CloudEventsTool;
import com.masterclass.microservices.enterprise.integration.SpringIntegrationTool;
import com.masterclass.microservices.messaging.activemq.ActiveMqTool;
import com.masterclass.microservices.messaging.azure.AzureServiceBusTool;
import com.masterclass.microservices.messaging.kafka.KafkaTool;
import com.masterclass.microservices.messaging.nats.NatsTool;
import com.masterclass.microservices.messaging.pulsar.PulsarTool;
import com.masterclass.microservices.messaging.rabbitmq.RabbitMqTool;
import com.masterclass.microservices.messaging.redis.RedisPubSubTool;
import com.masterclass.microservices.messaging.redis.RedisStreamsTool;
import com.masterclass.microservices.messaging.sqs.SqsSnsTool;
import com.masterclass.microservices.servicemesh.dapr.DaprTool;
import com.masterclass.microservices.servicemesh.feign.ExternalServiceTool;
import com.masterclass.microservices.servicemesh.graphql.GraphQlTool;
import com.masterclass.microservices.servicemesh.grpc.GrpcInventoryTool;
import com.masterclass.microservices.servicemesh.temporal.TemporalTool;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorAgentService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorAgentService.class);

    private static final String SYSTEM_PROMPT = """
            You are an expert microservices integration agent. Your role is to help users
            interact with various backend systems, message queues, databases, and services.

            You have access to the following integration tools organized by category:

            MESSAGING:
            - RabbitMQ: publishToRabbitMq, requestReplyRabbitMq (AMQP, work queues, request/reply)
            - Kafka: publishToKafka (event streaming, fan-out, audit log, replay)
            - Redis Streams: appendToRedisStream, readFromRedisStream (persistent ordered log)
            - Redis Pub/Sub: publishToRedisPubSub (real-time broadcast)
            - NATS: publishToNats, requestFromNats (ultra-low-latency messaging, RPC)
            - Pulsar: publishToPulsar (multi-tenant cloud-native streaming)
            - ActiveMQ/JMS: sendToJmsQueue, publishToJmsTopic (enterprise JMS 2.0)
            - Azure Service Bus: sendToAzureServiceBus (enterprise cloud messaging)
            - AWS SQS/SNS: sendToSqs, publishToSns (serverless AWS queuing and fan-out)

            DATABASES:
            - PostgreSQL: getPostgresSchema, queryPostgres (relational SQL, ACID)
            - MongoDB: listMongoCollections, queryMongoDB, insertMongoDocument (document store)
            - Cassandra: getCassandraSchema, queryCassandra (wide-column, high write throughput)
            - Elasticsearch: searchElasticsearch, indexDocument (full-text search, log analytics)
            - Neo4j: getNeo4jSchema, queryCypher (graph database, relationship traversal)
            - ClickHouse: queryClickHouse (OLAP analytics, columnar, billions of rows)
            - DynamoDB: getDynamoDbItem, putDynamoDbItem (serverless AWS NoSQL, key-value)

            CACHE:
            - Redis Semantic Cache: getCachedResponse, cacheResponse (LLM response memoization)
            - Hazelcast: putInHazelcast, getFromHazelcast (distributed in-memory data grid)

            SERVICE MESH:
            - gRPC: checkInventoryStock, reserveInventory (binary RPC, internal microservices)
            - GraphQL: executeGraphQlQuery (flexible query API)
            - OpenFeign REST: getProductPrice, getUserProfile (type-safe HTTP clients)
            - Dapr: publishViaDapr, invokeViaDapr (cloud-agnostic portable building blocks)
            - Temporal: startTemporalWorkflow (durable long-running agent workflows)

            ENTERPRISE INTEGRATION:
            - Apache Camel: routeViaCamel (300+ connectors, Enterprise Integration Patterns)
            - Spring Integration: sendToIntegrationPipeline (Spring-native EIP pipelines)
            - CloudEvents: publishCloudEvent (CNCF standard interoperable events)

            RULES:
            1. Always check schema/collections/labels BEFORE executing a database query.
            2. Check the Redis cache BEFORE calling any LLM-expensive operation.
            3. Use the most appropriate integration based on the use case described.
            4. For queries that need multiple systems, chain the tools logically.
            5. Return clear, structured responses explaining what was done and why.
            """;

    private final ChatClient chatClient;
    private final Timer agentTimer;

    // Messaging tools
    private final RabbitMqTool rabbitMqTool;
    private final KafkaTool kafkaTool;
    private final RedisStreamsTool redisStreamsTool;
    private final RedisPubSubTool redisPubSubTool;
    private final NatsTool natsTool;
    private final PulsarTool pulsarTool;
    private final ActiveMqTool activeMqTool;
    private final AzureServiceBusTool azureServiceBusTool;
    private final SqsSnsTool sqsSnsTool;

    // Database tools
    private final PostgresTool postgresTool;
    private final MongoTool mongoTool;
    private final CassandraTool cassandraTool;
    private final ElasticsearchTool elasticsearchTool;
    private final Neo4jTool neo4jTool;
    private final ClickHouseTool clickHouseTool;
    private final DynamoDbTool dynamoDbTool;

    // Cache tools
    private final RedisSemanticCacheTool redisSemanticCacheTool;
    private final HazelcastTool hazelcastTool;

    // Service mesh tools
    private final GrpcInventoryTool grpcInventoryTool;
    private final GraphQlTool graphQlTool;
    private final ExternalServiceTool externalServiceTool;
    private final DaprTool daprTool;
    private final TemporalTool temporalTool;

    // Enterprise tools
    private final CamelRouteTool camelRouteTool;
    private final SpringIntegrationTool springIntegrationTool;
    private final CloudEventsTool cloudEventsTool;

    public OrchestratorAgentService(
            ChatClient.Builder chatClientBuilder,
            MeterRegistry meterRegistry,
            RabbitMqTool rabbitMqTool, KafkaTool kafkaTool,
            RedisStreamsTool redisStreamsTool, RedisPubSubTool redisPubSubTool,
            NatsTool natsTool, PulsarTool pulsarTool, ActiveMqTool activeMqTool,
            AzureServiceBusTool azureServiceBusTool, SqsSnsTool sqsSnsTool,
            PostgresTool postgresTool, MongoTool mongoTool, CassandraTool cassandraTool,
            ElasticsearchTool elasticsearchTool, Neo4jTool neo4jTool,
            ClickHouseTool clickHouseTool, DynamoDbTool dynamoDbTool,
            RedisSemanticCacheTool redisSemanticCacheTool, HazelcastTool hazelcastTool,
            GrpcInventoryTool grpcInventoryTool, GraphQlTool graphQlTool,
            ExternalServiceTool externalServiceTool, DaprTool daprTool,
            TemporalTool temporalTool, CamelRouteTool camelRouteTool,
            SpringIntegrationTool springIntegrationTool, CloudEventsTool cloudEventsTool) {

        this.chatClient = chatClientBuilder.build();
        this.agentTimer = meterRegistry.timer("agent.orchestrator.latency");

        this.rabbitMqTool = rabbitMqTool;
        this.kafkaTool = kafkaTool;
        this.redisStreamsTool = redisStreamsTool;
        this.redisPubSubTool = redisPubSubTool;
        this.natsTool = natsTool;
        this.pulsarTool = pulsarTool;
        this.activeMqTool = activeMqTool;
        this.azureServiceBusTool = azureServiceBusTool;
        this.sqsSnsTool = sqsSnsTool;
        this.postgresTool = postgresTool;
        this.mongoTool = mongoTool;
        this.cassandraTool = cassandraTool;
        this.elasticsearchTool = elasticsearchTool;
        this.neo4jTool = neo4jTool;
        this.clickHouseTool = clickHouseTool;
        this.dynamoDbTool = dynamoDbTool;
        this.redisSemanticCacheTool = redisSemanticCacheTool;
        this.hazelcastTool = hazelcastTool;
        this.grpcInventoryTool = grpcInventoryTool;
        this.graphQlTool = graphQlTool;
        this.externalServiceTool = externalServiceTool;
        this.daprTool = daprTool;
        this.temporalTool = temporalTool;
        this.camelRouteTool = camelRouteTool;
        this.springIntegrationTool = springIntegrationTool;
        this.cloudEventsTool = cloudEventsTool;
    }

    public AgentResponse execute(String userMessage) {
        return agentTimer.record(() -> {
            log.debug("OrchestratorAgent executing: messageLength={}", userMessage.length());

            var response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .tools(
                            // Messaging
                            rabbitMqTool, kafkaTool, redisStreamsTool, redisPubSubTool,
                            natsTool, pulsarTool, activeMqTool, azureServiceBusTool, sqsSnsTool,
                            // Databases
                            postgresTool, mongoTool, cassandraTool, elasticsearchTool,
                            neo4jTool, clickHouseTool, dynamoDbTool,
                            // Cache
                            redisSemanticCacheTool, hazelcastTool,
                            // Service mesh
                            grpcInventoryTool, graphQlTool, externalServiceTool, daprTool, temporalTool,
                            // Enterprise
                            camelRouteTool, springIntegrationTool, cloudEventsTool
                    )
                    .call();

            String content = response.content();
            var chatResponse = response.chatResponse();
            Usage usage = chatResponse.getMetadata().getUsage();

            log.debug("OrchestratorAgent completed: promptTokens={} completionTokens={}",
                    usage.getPromptTokens(), usage.getCompletionTokens());

            return AgentResponse.of(content, "orchestrator",
                    usage.getPromptTokens(), usage.getCompletionTokens());
        });
    }
}
