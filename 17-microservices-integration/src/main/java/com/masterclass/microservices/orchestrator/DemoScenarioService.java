package com.masterclass.microservices.orchestrator;

import com.masterclass.microservices.cache.redis.RedisSemanticCacheTool;
import com.masterclass.microservices.databases.elasticsearch.ElasticsearchTool;
import com.masterclass.microservices.databases.mongodb.MongoTool;
import com.masterclass.microservices.databases.postgres.PostgresTool;
import com.masterclass.microservices.messaging.kafka.KafkaTool;
import com.masterclass.microservices.messaging.rabbitmq.RabbitMqTool;
import com.masterclass.microservices.messaging.redis.RedisStreamsTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates concrete integration scenarios by chaining tools directly.
 * Each method shows a realistic pattern that the orchestrator agent would follow.
 */
@Service
public class DemoScenarioService {

    private static final Logger log = LoggerFactory.getLogger(DemoScenarioService.class);

    private final ChatClient chatClient;
    private final KafkaTool kafkaTool;
    private final RabbitMqTool rabbitMqTool;
    private final RedisStreamsTool redisStreamsTool;
    private final RedisSemanticCacheTool redisCache;
    private final PostgresTool postgresTool;
    private final MongoTool mongoTool;
    private final ElasticsearchTool elasticsearchTool;

    public DemoScenarioService(
            ChatClient.Builder chatClientBuilder,
            KafkaTool kafkaTool, RabbitMqTool rabbitMqTool,
            RedisStreamsTool redisStreamsTool, RedisSemanticCacheTool redisCache,
            PostgresTool postgresTool, MongoTool mongoTool,
            ElasticsearchTool elasticsearchTool) {
        this.chatClient = chatClientBuilder.build();
        this.kafkaTool = kafkaTool;
        this.rabbitMqTool = rabbitMqTool;
        this.redisStreamsTool = redisStreamsTool;
        this.redisCache = redisCache;
        this.postgresTool = postgresTool;
        this.mongoTool = mongoTool;
        this.elasticsearchTool = elasticsearchTool;
    }

    /**
     * ORDER PIPELINE DEMO
     *
     * Simulates an e-commerce order event flowing through the system:
     * 1. Publish the raw order event to Kafka (durable event log)
     * 2. Classify the order via LLM (priority, risk level)
     * 3. Append enriched event to Redis Stream (lightweight ordered log)
     * 4. Cache the classification result in Redis (avoid re-classifying same order type)
     * 5. Publish async task to RabbitMQ (trigger fulfillment worker)
     * 6. Query Postgres for schema context (relational ACID store)
     */
    public DemoResult orderPipeline(String orderId, String orderPayload) {
        long start = System.currentTimeMillis();
        List<DemoResult.DemoStep> steps = new ArrayList<>();
        String prompt = "classify-order:" + orderId;

        // Step 1 — Kafka: durable event publish
        String kafkaResult = safeRun(() -> kafkaTool.publishToKafka(
                "ORDER_RECEIVED",
                """
                {"orderId":"%s","payload":%s}""".formatted(orderId, orderPayload)
        ));
        steps.add(DemoResult.DemoStep.ok("kafka", "publishToKafka(ORDER_RECEIVED)", kafkaResult));

        // Step 2 — Redis cache check (skip LLM if we've classified this order type before)
        String cached = safeRun(() -> redisCache.getCachedResponse(prompt));
        String classification;
        if (!"CACHE_MISS".equals(cached)) {
            classification = cached;
            steps.add(DemoResult.DemoStep.ok("redis-cache", "getCachedResponse → HIT", classification));
        } else {
            steps.add(DemoResult.DemoStep.ok("redis-cache", "getCachedResponse → MISS", "Proceeding to LLM"));

            // Step 3 — LLM classification
            classification = safeRun(() -> chatClient.prompt()
                    .system("""
                            Classify an order event. Respond ONLY with valid JSON:
                            {"priority":"HIGH|MEDIUM|LOW","risk":"HIGH|MEDIUM|LOW","action":"APPROVE|REVIEW|REJECT"}
                            """)
                    .user("Order payload: " + orderPayload)
                    .call()
                    .content());
            steps.add(DemoResult.DemoStep.ok("llm", "classify order payload", classification));

            // Step 4 — Cache the classification
            String cacheResult = safeRun(() -> redisCache.cacheResponse(prompt, classification));
            steps.add(DemoResult.DemoStep.ok("redis-cache", "cacheResponse(30 min TTL)", cacheResult));
        }

        // Step 5 — Redis Stream: append enriched event
        String streamResult = safeRun(() -> redisStreamsTool.appendToRedisStream(
                "ORDER_ENRICHED",
                """
                {"orderId":"%s","classification":%s}""".formatted(orderId, classification)
        ));
        steps.add(DemoResult.DemoStep.ok("redis-streams", "appendToRedisStream(ORDER_ENRICHED)", streamResult));

        // Step 6 — RabbitMQ: dispatch fulfillment task to worker queue
        String rabbitResult = safeRun(() -> rabbitMqTool.publishToRabbitMq(
                """
                {"task":"FULFILL_ORDER","orderId":"%s","priority":"%s"}""".formatted(
                        orderId, extractField(classification, "priority"))
        ));
        steps.add(DemoResult.DemoStep.ok("rabbitmq", "publishToRabbitMq(FULFILL_ORDER)", rabbitResult));

        // Step 7 — Postgres schema introspection (shows relational context)
        String schema = safeRun(postgresTool::getPostgresSchema);
        steps.add(DemoResult.DemoStep.ok("postgres", "getPostgresSchema()", truncate(schema, 200)));

        return new DemoResult(
                "order-pipeline",
                "Raw order event → Kafka → LLM classification → Redis cache → Redis Stream → RabbitMQ fulfillment",
                steps,
                "Order %s classified and dispatched. Classification: %s".formatted(orderId, truncate(classification, 150)),
                System.currentTimeMillis() - start,
                Instant.now()
        );
    }

    /**
     * CACHE-FIRST DEMO
     *
     * Demonstrates the cache-first pattern that cuts LLM costs for repeated queries:
     * 1. First call: CACHE_MISS → LLM call → cache the result
     * 2. Second call (same prompt): CACHE_HIT → return instantly, zero LLM tokens
     */
    public DemoResult cacheFirstPattern(String userQuestion) {
        long start = System.currentTimeMillis();
        List<DemoResult.DemoStep> steps = new ArrayList<>();

        // Round 1 — cold cache
        String miss = safeRun(() -> redisCache.getCachedResponse(userQuestion));
        steps.add(DemoResult.DemoStep.ok("redis-cache", "getCachedResponse [ROUND 1]", miss));

        String answer = safeRun(() -> chatClient.prompt()
                .system("You are a concise technical assistant. Answer in 1-2 sentences.")
                .user(userQuestion)
                .call()
                .content());
        steps.add(DemoResult.DemoStep.ok("llm", "chat completion (cache miss path)", truncate(answer, 200)));

        String stored = safeRun(() -> redisCache.cacheResponse(userQuestion, answer));
        steps.add(DemoResult.DemoStep.ok("redis-cache", "cacheResponse(result)", stored));

        // Round 2 — warm cache (same question, no LLM call)
        String hit = safeRun(() -> redisCache.getCachedResponse(userQuestion));
        steps.add(DemoResult.DemoStep.ok("redis-cache", "getCachedResponse [ROUND 2 — should be HIT]", truncate(hit, 200)));

        boolean hitSuccess = !"CACHE_MISS".equals(hit);
        String summary = hitSuccess
                ? "Cache-first working: Round 1 called LLM, Round 2 returned cached answer in ~1ms"
                : "Cache was not populated (Redis may be unavailable in this environment)";

        return new DemoResult(
                "cache-first",
                "Demonstrate LLM response memoization: first call hits LLM, subsequent calls return instantly",
                steps,
                summary,
                System.currentTimeMillis() - start,
                Instant.now()
        );
    }

    /**
     * MULTI-BROKER FAN-OUT DEMO
     *
     * Publishes a single event to 3 brokers simultaneously, demonstrating
     * when you'd route to different brokers for different consumer needs:
     * - Kafka: audit log consumers (durable, replayable)
     * - RabbitMQ: work queue consumers (load-balanced workers)
     * - Redis Streams: real-time dashboard consumers (lightweight, in-memory)
     */
    public DemoResult multiBrokerFanout(String eventType, String eventPayload) {
        long start = System.currentTimeMillis();
        List<DemoResult.DemoStep> steps = new ArrayList<>();

        // Kafka — durable audit log
        String kafka = safeRun(() -> kafkaTool.publishToKafka(eventType, eventPayload));
        steps.add(DemoResult.DemoStep.ok("kafka", "publishToKafka (audit log)", kafka));

        // RabbitMQ — work queue for downstream workers
        String rabbit = safeRun(() -> rabbitMqTool.publishToRabbitMq(
                """
                {"eventType":"%s","payload":%s}""".formatted(eventType, eventPayload)));
        steps.add(DemoResult.DemoStep.ok("rabbitmq", "publishToRabbitMq (worker queue)", rabbit));

        // Redis Streams — lightweight ordered log for dashboards
        String stream = safeRun(() -> redisStreamsTool.appendToRedisStream(eventType, eventPayload));
        steps.add(DemoResult.DemoStep.ok("redis-streams", "appendToRedisStream (dashboard log)", stream));

        // Read back from Redis Stream to prove durability
        String readback = safeRun(redisStreamsTool::readFromRedisStream);
        steps.add(DemoResult.DemoStep.ok("redis-streams", "readFromRedisStream (verify persistence)", truncate(readback, 300)));

        return new DemoResult(
                "multi-broker-fanout",
                "Single event fan-out to Kafka (audit) + RabbitMQ (workers) + Redis Streams (dashboard)",
                steps,
                "Event '%s' published to 3 brokers. Each serves a different consumer pattern.".formatted(eventType),
                System.currentTimeMillis() - start,
                Instant.now()
        );
    }

    /**
     * POLYGLOT PERSISTENCE DEMO
     *
     * Shows how the agent introspects multiple database engines simultaneously,
     * then uses the LLM to synthesize a query strategy across them.
     */
    public DemoResult polyglotPersistence(String dataQuestion) {
        long start = System.currentTimeMillis();
        List<DemoResult.DemoStep> steps = new ArrayList<>();

        // Gather schema/structure from each store
        String pgSchema = safeRun(postgresTool::getPostgresSchema);
        steps.add(DemoResult.DemoStep.ok("postgres", "getPostgresSchema()", truncate(pgSchema, 200)));

        String mongoCollections = safeRun(mongoTool::listMongoCollections);
        steps.add(DemoResult.DemoStep.ok("mongodb", "listMongoCollections()", truncate(mongoCollections, 200)));

        String esResult = safeRun(() -> elasticsearchTool.searchElasticsearch("agent-logs", dataQuestion));
        steps.add(DemoResult.DemoStep.ok("elasticsearch", "searchElasticsearch('agent-logs')", truncate(esResult, 200)));

        // LLM synthesizes which database to use and why
        String strategy = safeRun(() -> chatClient.prompt()
                .system("""
                        You are a database advisor. Given the user's question and available data stores,
                        recommend WHICH database to query and WHY. Respond in 2-3 sentences.
                        """)
                .user("""
                        User question: %s

                        PostgreSQL tables: %s
                        MongoDB collections: %s
                        Elasticsearch result preview: %s
                        """.formatted(dataQuestion,
                        truncate(pgSchema, 300),
                        truncate(mongoCollections, 200),
                        truncate(esResult, 200)))
                .call()
                .content());
        steps.add(DemoResult.DemoStep.ok("llm", "synthesize database strategy", strategy));

        return new DemoResult(
                "polyglot-persistence",
                "Introspect PostgreSQL + MongoDB + Elasticsearch then LLM recommends the best store",
                steps,
                strategy,
                System.currentTimeMillis() - start,
                Instant.now()
        );
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String safeRun(java.util.function.Supplier<String> action) {
        try {
            return action.get();
        } catch (Exception e) {
            log.warn("Demo step failed: {}", e.getMessage());
            return "unavailable: " + e.getMessage();
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private String extractField(String json, String field) {
        try {
            int idx = json.indexOf("\"" + field + "\":");
            if (idx < 0) return "MEDIUM";
            int start = json.indexOf('"', idx + field.length() + 3) + 1;
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        } catch (Exception e) {
            return "MEDIUM";
        }
    }
}
