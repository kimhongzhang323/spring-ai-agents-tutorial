package com.masterclass.knowledgegraph;

import com.masterclass.knowledgegraph.graph.GraphEngine;
import com.masterclass.knowledgegraph.graph.GraphState;
import com.masterclass.knowledgegraph.graph.RunResult;
import com.masterclass.knowledgegraph.model.KgEntity;
import com.masterclass.knowledgegraph.model.KgRelation;
import com.masterclass.knowledgegraph.model.KnowledgeGraph;
import com.masterclass.knowledgegraph.service.KnowledgeGraphService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KnowledgeGraphService using a stubbed ChatClient.
 *
 * Strategy: use a test-double ChatClient that returns deterministic LLM output,
 * then assert on the RunResult state rather than the LLM content.
 */
class KnowledgeGraphServiceTest {

    // ── Build pipeline: completes when LLM returns valid entity + relation lines ──

    @Test
    void buildGraph_completedResult_whenLlmReturnsValidOutput() {
        ChatClient.Builder builder = stubbedBuilder(
                // ExtractEntitiesNode call
                "ENTITY|Person|Alan Turing|per:alan-turing\nENTITY|Organization|Bletchley Park|org:bletchley",
                // ExtractRelationsNode call
                "RELATION|per:alan-turing|WORKS_AT|org:bletchley|0.95",
                // SummariseGraphNode call
                "Alan Turing worked at Bletchley Park."
        );

        KnowledgeGraphService service = new KnowledgeGraphService(builder, new SimpleMeterRegistry());
        RunResult result = service.buildGraph("Alan Turing worked at Bletchley Park during WWII.");

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.finalState().<String>require("graph_summary"))
                .contains("Alan Turing");
    }

    // ── Build pipeline: returns COMPLETED even when entities are extracted but no relations ──

    @Test
    void buildGraph_noRelations_stillCompletes() {
        ChatClient.Builder builder = stubbedBuilder(
                "ENTITY|Person|Alan Turing|per:alan-turing",
                "",  // no relations
                "A graph with one entity."
        );

        KnowledgeGraphService service = new KnowledgeGraphService(builder, new SimpleMeterRegistry());
        RunResult result = service.buildGraph("Alan Turing.");

        assertThat(result.isCompleted()).isTrue();
    }

    // ── Query pipeline: returns COMPLETED with an answer key ──────────────────

    @Test
    void query_returnsCompletedResult() {
        // Pre-populate graph by building first, then query
        ChatClient.Builder builder = stubbedBuilder(
                "ENTITY|Person|Alan Turing|per:alan-turing",
                "",
                "Graph summary.",
                // QueryGraphNode call
                "Alan Turing worked at Bletchley Park."
        );

        KnowledgeGraphService service = new KnowledgeGraphService(builder, new SimpleMeterRegistry());
        service.buildGraph("Alan Turing.");

        RunResult queryResult = service.query("Who is Alan Turing?");
        assertThat(queryResult.isCompleted()).isTrue();
        assertThat(queryResult.finalState().<String>require("answer")).isNotBlank();
    }

    // ── Resume: unknown threadId returns ERROR ────────────────────────────────

    @Test
    void resumeBuild_unknownThreadId_returnsError() {
        ChatClient.Builder builder = stubbedBuilder();
        KnowledgeGraphService service = new KnowledgeGraphService(builder, new SimpleMeterRegistry());

        RunResult result = service.resumeBuild("non-existent-thread-id", "approve");
        assertThat(result.status()).isEqualTo(RunResult.Status.ERROR);
        assertThat(result.error()).contains("No suspended run");
    }

    // ── KnowledgeGraph inspect: starts empty ─────────────────────────────────

    @Test
    void getKnowledgeGraph_startsEmpty() {
        ChatClient.Builder builder = stubbedBuilder();
        KnowledgeGraphService service = new KnowledgeGraphService(builder, new SimpleMeterRegistry());

        KnowledgeGraph kg = service.getKnowledgeGraph();
        assertThat(kg.entityCount()).isZero();
        assertThat(kg.relationCount()).isZero();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns a ChatClient.Builder whose built client returns each response string
     * in order on successive .call().content() invocations.
     */
    @SuppressWarnings("unchecked")
    private static ChatClient.Builder stubbedBuilder(String... responses) {
        ChatClient.Builder builder      = mock(ChatClient.Builder.class);
        ChatClient         chatClient   = mock(ChatClient.class);

        var promptSpec   = mock(ChatClient.ChatClientRequestSpec.class);
        var callSpec     = mock(ChatClient.CallResponseSpec.class);

        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(any(String.class))).thenReturn(promptSpec);
        when(promptSpec.user(any(String.class))).thenReturn(promptSpec);

        if (responses.length == 0) {
            when(callSpec.content()).thenReturn("");
        } else if (responses.length == 1) {
            when(callSpec.content()).thenReturn(responses[0]);
        } else {
            String first = responses[0];
            String[] rest = new String[responses.length - 1];
            System.arraycopy(responses, 1, rest, 0, rest.length);
            when(callSpec.content()).thenReturn(first, rest);
        }
        when(promptSpec.call()).thenReturn(callSpec);
        return builder;
    }
}
