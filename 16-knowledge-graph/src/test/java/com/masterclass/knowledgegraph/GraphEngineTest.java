package com.masterclass.knowledgegraph;

import com.masterclass.knowledgegraph.graph.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class GraphEngineTest {

    // ── Happy path: linear graph completes ──────────────────────────────────

    @Test
    void linearGraph_completesSuccessfully() {
        GraphEngine engine = GraphEngine.builder()
                .node("nodeA", state -> GraphState.of(Map.of("a", "done")))
                .node("nodeB", state -> GraphState.of(Map.of("b", state.<String>require("a") + "+b")))
                .edge("nodeA", "nodeB")
                .edge("nodeB", GraphEngine.END)
                .entryPoint("nodeA")
                .build();

        RunResult result = engine.run(GraphState.empty());

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.finalState().<String>require("b")).isEqualTo("done+b");
    }

    // ── Conditional routing ──────────────────────────────────────────────────

    @Test
    void conditionalEdge_routesToCorrectNode() {
        AtomicInteger routeACount = new AtomicInteger();
        AtomicInteger routeBCount = new AtomicInteger();

        GraphEngine engine = GraphEngine.builder()
                .node("router",  state -> GraphState.of(Map.of("score", 8)))
                .node("highPath", state -> { routeACount.incrementAndGet(); return GraphState.of(Map.of("result", "high")); })
                .node("lowPath",  state -> { routeBCount.incrementAndGet(); return GraphState.of(Map.of("result", "low")); })
                .conditionalEdge("router", state -> {
                    int score = state.require("score");
                    return score >= 7 ? "highPath" : "lowPath";
                })
                .edge("highPath", GraphEngine.END)
                .edge("lowPath",  GraphEngine.END)
                .entryPoint("router")
                .build();

        RunResult result = engine.run(GraphState.empty());

        assertThat(result.isCompleted()).isTrue();
        assertThat(routeACount.get()).isEqualTo(1);
        assertThat(routeBCount.get()).isEqualTo(0);
        assertThat(result.finalState().<String>require("result")).isEqualTo("high");
    }

    // ── Human-in-the-loop interrupt ──────────────────────────────────────────

    @Test
    void interruptAndResume_completesAfterApproval() {
        AtomicInteger afterInterruptCount = new AtomicInteger();

        GraphEngine engine = GraphEngine.builder()
                .node("step1", state -> GraphState.of(Map.of("step1", true)))
                .node("step2", state -> {
                    String humanResponse = state.<String>get("human_response").orElse("");
                    if (!humanResponse.equalsIgnoreCase("approve"))
                        throw new InterruptException("Please approve to continue");
                    afterInterruptCount.incrementAndGet();
                    return GraphState.of(Map.of("step2", true));
                })
                .edge("step1", "step2")
                .edge("step2", GraphEngine.END)
                .entryPoint("step1")
                .build();

        // First run — hits the interrupt
        RunResult suspended = engine.run(GraphState.empty());
        assertThat(suspended.isSuspended()).isTrue();
        assertThat(suspended.interruptPrompt()).contains("approve");

        // Resume with approval
        RunResult completed = engine.resume(suspended.threadId(), "approve");
        assertThat(completed.isCompleted()).isTrue();
        assertThat(afterInterruptCount.get()).isEqualTo(1);
    }

    // ── State channel merge (append semantics) ───────────────────────────────

    @Test
    void stateChannelMerge_appendsLists() {
        GraphState s1 = GraphState.of(Map.of("items", java.util.List.of("a")));
        GraphState s2 = s1.merge(Map.of("items", java.util.List.of("b", "c")));

        assertThat(s2.<java.util.List<String>>require("items"))
                .containsExactly("a", "b", "c");
    }

    // ── Max steps guard ───────────────────────────────────────────────────────

    @Test
    void maxStepsExceeded_returnsError() {
        // Build an unconditional cycle that never terminates
        // Note: cycle detection prevents unconditional cycles at build time,
        // so we simulate with a conditional that always loops back
        GraphEngine engine = GraphEngine.builder()
                .node("looper", state -> {
                    int count = state.<Integer>get("count").orElse(0);
                    return GraphState.of(Map.of("count", count + 1));
                })
                .conditionalEdge("looper", state -> "looper")  // always loop back
                .entryPoint("looper")
                .maxSteps(5)
                .build();

        RunResult result = engine.run(GraphState.empty());
        assertThat(result.status()).isEqualTo(RunResult.Status.ERROR);
        assertThat(result.error()).contains("Max steps");
    }

    // ── Cycle detection at build time ────────────────────────────────────────

    @Test
    void unconditionalCycle_throwsAtBuildTime() {
        assertThatThrownBy(() ->
                GraphEngine.builder()
                        .node("a", s -> s)
                        .node("b", s -> s)
                        .edge("a", "b")
                        .edge("b", "a")
                        .entryPoint("a")
                        .build()
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("cycle");
    }
}
