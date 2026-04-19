package com.masterclass.knowledgegraph.controller;

import com.masterclass.knowledgegraph.graph.RunResult;
import com.masterclass.knowledgegraph.model.KnowledgeGraph;
import com.masterclass.knowledgegraph.service.KnowledgeGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the knowledge-graph agent pipeline.
 *
 * POST /api/v1/knowledge-graph/build      — ingest text and build the graph
 * POST /api/v1/knowledge-graph/resume     — resume a suspended (human-in-the-loop) run
 * POST /api/v1/knowledge-graph/query      — query the graph with natural language
 * GET  /api/v1/knowledge-graph/inspect    — inspect current graph statistics
 */
@RestController
@RequestMapping("/api/v1/knowledge-graph")
@Tag(name = "Knowledge Graph", description = "LangGraph-style stateful agent pipeline for building and querying knowledge graphs")
public class KnowledgeGraphController {

    private final KnowledgeGraphService kgService;

    public KnowledgeGraphController(KnowledgeGraphService kgService) {
        this.kgService = kgService;
    }

    @PostMapping("/build")
    @Operation(
            summary = "Build knowledge graph from text",
            description = "Runs the full extraction pipeline: entities → relations → validation → summary. " +
                          "If validation triggers a human-in-the-loop interrupt, returns status=SUSPENDED with a threadId. " +
                          "Use /resume to continue the run."
    )
    public ResponseEntity<GraphResponse> build(
            @Valid @RequestBody BuildRequest request,
            @AuthenticationPrincipal UserDetails user) {

        RunResult result = kgService.buildGraph(request.text());
        GraphResponse response = GraphResponse.from(result);

        if (result.status() == RunResult.Status.ERROR) {
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resume")
    @Operation(
            summary = "Resume a suspended graph build",
            description = "Supply threadId from a SUSPENDED build response. " +
                          "response must be 'approve' (continue) or 'reject' (abort)."
    )
    public ResponseEntity<GraphResponse> resume(
            @Valid @RequestBody ResumeRequest request,
            @AuthenticationPrincipal UserDetails user) {

        RunResult result = kgService.resumeBuild(request.threadId(), request.response());
        return ResponseEntity.ok(GraphResponse.from(result));
    }

    @PostMapping("/query")
    @Operation(
            summary = "Query the knowledge graph",
            description = "Ask a natural language question. The agent performs multi-hop graph traversal " +
                          "and returns a grounded answer with the supporting graph context."
    )
    public ResponseEntity<GraphResponse> query(
            @Valid @RequestBody QueryRequest request,
            @AuthenticationPrincipal UserDetails user) {

        RunResult result = kgService.query(request.query());
        if (result.status() == RunResult.Status.ERROR) {
            return ResponseEntity.internalServerError().body(GraphResponse.from(result));
        }
        return ResponseEntity.ok(GraphResponse.from(result));
    }

    @GetMapping("/inspect")
    @Operation(summary = "Inspect current knowledge graph statistics")
    public ResponseEntity<Map<String, Object>> inspect(
            @AuthenticationPrincipal UserDetails user) {

        KnowledgeGraph kg = kgService.getKnowledgeGraph();
        return ResponseEntity.ok(Map.of(
                "entityCount",   kg.entityCount(),
                "relationCount", kg.relationCount(),
                "graphDump",     kg.toContextString()
        ));
    }
}
