package com.masterclass.mcp.resource;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.server.McpServerFeatures;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Exposes static documentation as MCP Resources.
 *
 * Resources are content the LLM passively reads (API specs, architecture docs,
 * runbooks). Prefer Resources over Tools for stable, non-parameterised content.
 *
 * URI scheme: documents/{name}
 */
@Component
public class DocumentResourceProvider {

    public List<McpServerFeatures.SyncResourceRegistration> registrations() {
        return List.of(
                apiSpecResource(),
                architectureResource(),
                runbookResource()
        );
    }

    private McpServerFeatures.SyncResourceRegistration apiSpecResource() {
        var resource = new McpSchema.Resource(
                "documents/api-spec",
                "REST API Specification",
                "OpenAPI 3.1 specification for the Masterclass REST API",
                "text/markdown",
                null
        );
        return new McpServerFeatures.SyncResourceRegistration(resource, req -> {
            var content = new McpSchema.TextResourceContents(
                    req.uri(),
                    "text/markdown",
                    API_SPEC_MARKDOWN
            );
            return new McpSchema.ReadResourceResult(List.of(content));
        });
    }

    private McpServerFeatures.SyncResourceRegistration architectureResource() {
        var resource = new McpSchema.Resource(
                "documents/architecture",
                "System Architecture",
                "High-level architecture of the Java AI Agents Masterclass system",
                "text/markdown",
                null
        );
        return new McpServerFeatures.SyncResourceRegistration(resource, req -> {
            var content = new McpSchema.TextResourceContents(
                    req.uri(),
                    "text/markdown",
                    ARCHITECTURE_MARKDOWN
            );
            return new McpSchema.ReadResourceResult(List.of(content));
        });
    }

    private McpServerFeatures.SyncResourceRegistration runbookResource() {
        var resource = new McpSchema.Resource(
                "documents/runbook",
                "Operations Runbook",
                "Step-by-step runbook for common operational tasks",
                "text/markdown",
                null
        );
        return new McpServerFeatures.SyncResourceRegistration(resource, req -> {
            var content = new McpSchema.TextResourceContents(
                    req.uri(),
                    "text/markdown",
                    RUNBOOK_MARKDOWN
            );
            return new McpSchema.ReadResourceResult(List.of(content));
        });
    }

    private static final String API_SPEC_MARKDOWN = """
            # Masterclass API v1

            Base URL: `https://api.masterclass.example.com/api/v1`

            ## Authentication
            All endpoints require `Authorization: Bearer <jwt>`.

            ## Endpoints

            ### POST /agents/chat
            Chat with the AI agent.
            **Body**: `{ "message": "string" }`
            **Response**: `{ "response": "string", "tokens": { "prompt": 0, "completion": 0 } }`

            ### GET /agents/history/{sessionId}
            Retrieve conversation history for a session.

            ### POST /rag/ingest
            Ingest a document into the vector store (ADMIN only).
            **Body**: multipart/form-data with `file` field.
            """;

    private static final String ARCHITECTURE_MARKDOWN = """
            # System Architecture

            ## Layers
            1. **Controller** — HTTP concerns, OpenAPI docs, rate limiting
            2. **Service** — Business logic, orchestration
            3. **Agent** — LLM orchestration via ChatClient, tool routing
            4. **Tool** — Atomic, side-effecting operations

            ## Infrastructure
            - **Ollama** (local) / **OpenAI** (cloud) — LLM provider
            - **PGVector** — Vector store for RAG
            - **Redis** — Rate limit state, conversation memory
            - **Prometheus + Grafana** — Metrics and dashboards
            """;

    private static final String RUNBOOK_MARKDOWN = """
            # Operations Runbook

            ## Restart a module
            ```bash
            ./mvnw -pl 01-hello-agent spring-boot:run
            ```

            ## Clear Redis cache
            ```bash
            docker exec -it redis redis-cli FLUSHDB
            ```

            ## Check LLM connectivity
            ```bash
            curl http://localhost:11434/api/tags
            ```

            ## View metrics
            Open http://localhost:3000 (Grafana) → AI Agents dashboard
            """;
}
