package com.masterclass.knowledgegraph.service;

import com.masterclass.knowledgegraph.agent.*;
import com.masterclass.knowledgegraph.graph.GraphEngine;
import com.masterclass.knowledgegraph.graph.GraphState;
import com.masterclass.knowledgegraph.graph.RunResult;
import com.masterclass.knowledgegraph.model.KnowledgeGraph;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Orchestrates two graph pipelines:
 *
 *   BUILD pipeline:
 *     extract_entities → extract_relations → validate_graph → summarise_graph → END
 *     └─ conditional: if validation_status == "REJECTED" → END (abort)
 *
 *   QUERY pipeline:
 *     query_graph → END
 *
 * Both pipelines run on the same shared KnowledgeGraph instance, so the graph
 * is built once and queried many times.
 */
@Service
public class KnowledgeGraphService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphService.class);

    private final GraphEngine     buildPipeline;
    private final GraphEngine     queryPipeline;
    private final KnowledgeGraph  knowledgeGraph;
    private final MeterRegistry   meterRegistry;

    public KnowledgeGraphService(ChatClient.Builder chatClientBuilder,
                                 MeterRegistry meterRegistry) {
        this.meterRegistry  = meterRegistry;
        this.knowledgeGraph = new KnowledgeGraph();

        ChatClient chatClient = chatClientBuilder.build();

        // ── Build pipeline nodes ──────────────────────────────────────────
        ExtractEntitiesNode extractEntities   = new ExtractEntitiesNode(chatClient, knowledgeGraph);
        ExtractRelationsNode extractRelations = new ExtractRelationsNode(chatClient, knowledgeGraph);
        ValidateGraphNode   validateGraph     = new ValidateGraphNode(knowledgeGraph, 0.5, 2);
        SummariseGraphNode  summariseGraph    = new SummariseGraphNode(chatClient, knowledgeGraph);

        this.buildPipeline = GraphEngine.builder()
                .node("extract_entities",  extractEntities)
                .node("extract_relations", extractRelations)
                .node("validate_graph",    validateGraph)
                .node("summarise_graph",   summariseGraph)
                // Entity extraction feeds relation extraction
                .edge("extract_entities",  "extract_relations")
                // Relation extraction feeds validation
                .edge("extract_relations", "validate_graph")
                // Conditional: if rejected by human, abort
                .conditionalEdge("validate_graph", state -> {
                    String humanResponse = state.<String>get("human_response").orElse("approve");
                    if (humanResponse.equalsIgnoreCase("reject")) return GraphEngine.END;
                    return "summarise_graph";
                })
                .edge("summarise_graph", GraphEngine.END)
                .entryPoint("extract_entities")
                .maxSteps(20)
                .build();

        // ── Query pipeline ────────────────────────────────────────────────
        QueryGraphNode queryGraph = new QueryGraphNode(chatClient, knowledgeGraph);

        this.queryPipeline = GraphEngine.builder()
                .node("query_graph", queryGraph)
                .edge("query_graph", GraphEngine.END)
                .entryPoint("query_graph")
                .maxSteps(5)
                .build();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Ingests raw text, runs the full build pipeline, and returns a RunResult.
     * RunResult may be COMPLETED, SUSPENDED (human approval needed), or ERROR.
     */
    public RunResult buildGraph(String rawText) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Starting graph BUILD pipeline, text length={}", rawText.length());
            RunResult result = buildPipeline.run(GraphState.of(Map.of("input_text", rawText)));
            meterRegistry.counter("kg.build.total",
                    "status", result.status().name()).increment();
            return result;
        } finally {
            sample.stop(meterRegistry.timer("kg.build.duration"));
        }
    }

    /**
     * Resumes a suspended build pipeline (human-in-the-loop approval).
     *
     * @param threadId      from the suspended RunResult
     * @param humanResponse "approve" or "reject"
     */
    public RunResult resumeBuild(String threadId, String humanResponse) {
        log.info("Resuming graph BUILD pipeline threadId={} humanResponse={}", threadId, humanResponse);
        return buildPipeline.resume(threadId, humanResponse);
    }

    /**
     * Queries the knowledge graph with a natural language question.
     */
    public RunResult query(String question) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Starting graph QUERY pipeline, question='{}'", question);
            return queryPipeline.run(GraphState.of(Map.of("query", question)));
        } finally {
            sample.stop(meterRegistry.timer("kg.query.duration"));
        }
    }

    public KnowledgeGraph getKnowledgeGraph() {
        return knowledgeGraph;
    }
}
