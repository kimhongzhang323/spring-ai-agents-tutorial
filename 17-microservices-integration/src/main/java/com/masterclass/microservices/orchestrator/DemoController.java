package com.masterclass.microservices.orchestrator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Pre-scripted demo endpoints that show real integration patterns without needing
 * custom prompts. Each endpoint walks through a complete scenario step by step,
 * returning every tool invocation and its result so you can follow along in the UI.
 */
@RestController
@RequestMapping("/api/v1/agents/microservices/demo")
@Tag(name = "Integration Demos", description = "Pre-scripted end-to-end scenarios demonstrating tool chains")
@SecurityRequirement(name = "bearerAuth")
public class DemoController {

    private final DemoScenarioService demoService;

    public DemoController(DemoScenarioService demoService) {
        this.demoService = demoService;
    }

    @PostMapping("/order-pipeline")
    @Operation(
            summary = "Order pipeline demo",
            description = """
                    Simulates an e-commerce order flowing through the microservices stack:

                    1. **Kafka** — publish raw ORDER_RECEIVED event (durable, replayable audit log)
                    2. **Redis Cache** — check if we already classified this order type (avoid LLM cost)
                    3. **LLM** — classify order priority + risk (only if cache miss)
                    4. **Redis Cache** — store classification result with 30-min TTL
                    5. **Redis Streams** — append enriched ORDER_ENRICHED event (lightweight log)
                    6. **RabbitMQ** — dispatch FULFILL_ORDER task to worker queue (async decoupling)
                    7. **PostgreSQL** — introspect schema to show relational context

                    Each step returns the exact tool call result so you can trace the full pipeline.
                    """
    )
    public ResponseEntity<DemoResult> orderPipeline(
            @RequestParam(defaultValue = "ORD-2024-001") String orderId,
            @RequestParam(defaultValue = "{\"items\":[{\"sku\":\"LAPTOP-X1\",\"qty\":2,\"price\":1299.99}],\"total\":2599.98,\"currency\":\"USD\"}")
            @Parameter(description = "Order JSON payload") String payload) {
        return ResponseEntity.ok(demoService.orderPipeline(orderId, payload));
    }

    @PostMapping("/cache-first")
    @Operation(
            summary = "Cache-first LLM demo",
            description = """
                    Demonstrates the cache-first pattern that cuts LLM API costs for repeated queries:

                    **Round 1** (cold cache):
                    - Redis lookup → CACHE_MISS
                    - LLM call → answer generated (costs tokens)
                    - Redis set → answer stored with 30-min TTL

                    **Round 2** (warm cache, same question):
                    - Redis lookup → CACHE_HIT
                    - Return instantly — zero tokens consumed, ~1ms latency

                    This pattern is essential for high-traffic agents answering FAQ-style questions.
                    """
    )
    public ResponseEntity<DemoResult> cacheFirstPattern(
            @RequestParam(defaultValue = "What is Apache Kafka and when should I use it instead of RabbitMQ?")
            @Parameter(description = "The question to demonstrate cache round-trip for") String question) {
        return ResponseEntity.ok(demoService.cacheFirstPattern(question));
    }

    @PostMapping("/multi-broker-fanout")
    @Operation(
            summary = "Multi-broker fan-out demo",
            description = """
                    Publishes a single event to three different brokers simultaneously,
                    each serving a distinct consumer pattern:

                    - **Kafka** → audit log consumers (durable, offset-based replay, compliance)
                    - **RabbitMQ** → worker queue consumers (round-robin load balancing, backpressure)
                    - **Redis Streams** → real-time dashboard consumers (lightweight, no Zookeeper)

                    Then reads back from Redis Stream to prove persistence.
                    Use this pattern when one event must trigger multiple independent downstream systems.
                    """
    )
    public ResponseEntity<DemoResult> multiBrokerFanout(
            @RequestParam(defaultValue = "USER_SIGNUP") String eventType,
            @RequestParam(defaultValue = "{\"userId\":\"usr-42\",\"plan\":\"PRO\",\"source\":\"organic\"}")
            @Parameter(description = "Event JSON payload") String payload) {
        return ResponseEntity.ok(demoService.multiBrokerFanout(eventType, payload));
    }

    @PostMapping("/polyglot-persistence")
    @Operation(
            summary = "Polyglot persistence demo",
            description = """
                    Shows how the agent inspects multiple database engines in one request,
                    then asks the LLM to recommend the best store for a specific data question:

                    1. **PostgreSQL** — getPostgresSchema() — relational tables and columns
                    2. **MongoDB** — listMongoCollections() — document collections
                    3. **Elasticsearch** — searchElasticsearch() — full-text search results
                    4. **LLM** — synthesizes which database to use and explains why

                    This mirrors how a real AI agent navigates heterogeneous data environments.
                    """
    )
    public ResponseEntity<DemoResult> polyglotPersistence(
            @RequestParam(defaultValue = "Find all user activity logs from the last 24 hours with error severity")
            @Parameter(description = "Data question to route to the best database") String question) {
        return ResponseEntity.ok(demoService.polyglotPersistence(question));
    }
}
