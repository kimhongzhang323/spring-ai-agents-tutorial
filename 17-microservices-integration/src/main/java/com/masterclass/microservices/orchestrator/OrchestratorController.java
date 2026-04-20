package com.masterclass.microservices.orchestrator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/agents/microservices")
@Tag(name = "Microservices Integration Agent", description = "AI agent connected to 35+ industry microservice integrations")
@SecurityRequirement(name = "bearerAuth")
public class OrchestratorController {

    private final OrchestratorAgentService agentService;
    private final StreamingAgentService streamingService;

    public OrchestratorController(OrchestratorAgentService agentService,
                                   StreamingAgentService streamingService) {
        this.agentService = agentService;
        this.streamingService = streamingService;
    }

    @PostMapping("/chat")
    @Operation(
            summary = "Chat with the microservices integration agent",
            description = """
                    Sends a message to the orchestrator agent which can interact with:
                    - Message Queues: RabbitMQ, Kafka, NATS, Pulsar, ActiveMQ, Azure Service Bus, AWS SQS/SNS, Redis Streams
                    - Databases: PostgreSQL, MongoDB, Cassandra, Elasticsearch, Neo4j, ClickHouse, DynamoDB
                    - Caches: Redis semantic cache, Hazelcast distributed grid
                    - Services: gRPC, GraphQL, OpenFeign REST, Dapr, Temporal workflows
                    - Enterprise: Apache Camel, Spring Integration, CloudEvents

                    Authentication: JWT bearer token required.
                    Rate limit: 30 requests/min per user, 300/min global.
                    """
    )
    public ResponseEntity<AgentResponse> chat(
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        AgentResponse response = agentService.execute(request.message());
        return ResponseEntity.ok()
                .header("X-Integration-Used", response.integrationUsed())
                .header("X-Prompt-Tokens", String.valueOf(response.promptTokens()))
                .header("X-Completion-Tokens", String.valueOf(response.completionTokens()))
                .body(response);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Streaming chat with the microservices integration agent (SSE)",
            description = """
                    Same as /chat but streams the response token-by-token via Server-Sent Events (SSE).
                    Use this for long-running agent responses where the user should see partial output
                    immediately rather than waiting for the full response.

                    The stream emits plain text chunks. The connection closes when the agent finishes.
                    Tool invocations (e.g. Kafka publish, DB query) happen before streaming begins.
                    """
    )
    public Flux<String> chatStream(
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return streamingService.executeStreaming(request.message());
    }

    @GetMapping("/integrations")
    @Operation(
            summary = "List all available integrations",
            description = "Returns a catalog of all microservice integrations available to the agent."
    )
    public ResponseEntity<IntegrationCatalog> listIntegrations() {
        return ResponseEntity.ok(IntegrationCatalog.full());
    }
}
