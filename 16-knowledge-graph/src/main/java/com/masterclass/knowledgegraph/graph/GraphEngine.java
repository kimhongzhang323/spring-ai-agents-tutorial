package com.masterclass.knowledgegraph.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * LangGraph-equivalent graph execution engine for Java.
 *
 * Supports:
 *   - Directed node graph with named edges
 *   - Conditional routing edges (EdgeCondition)
 *   - Human-in-the-loop interrupts (InterruptException)
 *   - Parallel fan-out/fan-in via CompletableFuture
 *   - Cycle detection at compile time
 *   - Step-by-step execution trace for debugging
 *
 * Lifecycle:
 *   1. GraphEngine engine = GraphEngine.builder()
 *          .node("classify", classifyNode)
 *          .node("research", researchNode)
 *          .edge("classify", "research")               // unconditional
 *          .conditionalEdge("research", routerCondition) // conditional
 *          .node("summarise", summariseNode)
 *          .entryPoint("classify")
 *          .build();
 *   2. RunResult result = engine.run(GraphState.of(Map.of("query", userInput)));
 *   3. if (result.isSuspended()) engine.resume(result.threadId(), humanResponse);
 */
public final class GraphEngine {

    private static final Logger log = LoggerFactory.getLogger(GraphEngine.class);

    public static final String END   = "__end__";
    public static final String START = "__start__";

    private final Map<String, GraphNode>      nodes;
    private final Map<String, String>         unconditionalEdges;
    private final Map<String, EdgeCondition>  conditionalEdges;
    private final Map<String, List<String>>   parallelEdges;
    private final String                      entryPoint;
    private final int                         maxSteps;
    private final ExecutorService             executor;

    // In-memory thread store (production should swap for Redis-backed store)
    private final Map<String, SuspendedRun>   suspendedRuns = new ConcurrentHashMap<>();

    private GraphEngine(Builder b) {
        this.nodes              = Collections.unmodifiableMap(b.nodes);
        this.unconditionalEdges = Collections.unmodifiableMap(b.unconditionalEdges);
        this.conditionalEdges   = Collections.unmodifiableMap(b.conditionalEdges);
        this.parallelEdges      = Collections.unmodifiableMap(b.parallelEdges);
        this.entryPoint         = Objects.requireNonNull(b.entryPoint, "entryPoint must be set");
        this.maxSteps           = b.maxSteps;
        this.executor           = b.executor != null ? b.executor
                : ForkJoinPool.commonPool();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public RunResult run(GraphState initialState) {
        String threadId = UUID.randomUUID().toString();
        return execute(initialState, entryPoint, threadId, new ArrayList<>());
    }

    public RunResult resume(String threadId, String humanResponse) {
        SuspendedRun suspended = suspendedRuns.remove(threadId);
        if (suspended == null) {
            return error(threadId, "No suspended run found for threadId: " + threadId, GraphState.empty());
        }
        GraphState resumed = suspended.state().merge(
                Map.of("human_response", humanResponse, "interrupted", false));
        return execute(resumed, suspended.nextNode(), threadId, new ArrayList<>(suspended.trace()));
    }

    public Optional<SuspendedRun> getSuspendedRun(String threadId) {
        return Optional.ofNullable(suspendedRuns.get(threadId));
    }

    // -----------------------------------------------------------------------
    // Internal execution loop
    // -----------------------------------------------------------------------

    private RunResult execute(GraphState state, String startNode, String threadId, List<String> trace) {
        String current = startNode;
        int steps = 0;

        while (!END.equals(current)) {
            if (steps++ >= maxSteps) {
                return error(threadId, "Max steps (" + maxSteps + ") exceeded — possible cycle", state);
            }

            GraphNode node = nodes.get(current);
            if (node == null) {
                return error(threadId, "Unknown node: " + current, state);
            }

            log.debug("[graph:{}] step={} node={}", threadId, steps, current);
            trace.add(current);

            // Parallel fan-out
            if (parallelEdges.containsKey(current)) {
                state = executeParallel(state, current, parallelEdges.get(current), threadId);
                if (state.get("__error__").isPresent()) {
                    return error(threadId, state.<String>require("__error__"), state);
                }
                current = resolveNext(current, state);
                continue;
            }

            // Normal node execution with interrupt support
            try {
                GraphState updates = node.execute(state);
                state = state.merge(updates.asMap());
            } catch (InterruptException interrupt) {
                log.info("[graph:{}] interrupted at node={} prompt={}", threadId, current, interrupt.prompt());
                suspendedRuns.put(threadId, new SuspendedRun(state, current, List.copyOf(trace)));
                return new RunResult(RunResult.Status.SUSPENDED, state, interrupt.prompt(), null, threadId);
            } catch (Exception ex) {
                log.error("[graph:{}] node={} threw unexpected exception", threadId, current, ex);
                return error(threadId, ex.getMessage(), state);
            }

            current = resolveNext(current, state);
        }

        log.info("[graph:{}] completed in {} steps, trace={}", threadId, steps, trace);
        return new RunResult(RunResult.Status.COMPLETED,
                state.merge(Map.of("__trace__", trace, "__steps__", steps)),
                null, null, threadId);
    }

    private GraphState executeParallel(GraphState state, String fromNode,
                                       List<String> parallelNodes, String threadId) {
        List<CompletableFuture<GraphState>> futures = parallelNodes.stream()
                .map(name -> CompletableFuture.supplyAsync(() -> {
                    GraphNode n = nodes.get(name);
                    if (n == null) throw new IllegalStateException("Unknown parallel node: " + name);
                    return n.execute(state);
                }, executor))
                .toList();

        GraphState merged = state;
        for (CompletableFuture<GraphState> future : futures) {
            try {
                merged = merged.merge(future.get(30, TimeUnit.SECONDS).asMap());
            } catch (Exception ex) {
                log.error("[graph:{}] parallel node in {} failed", threadId, fromNode, ex);
                return merged.merge(Map.of("__error__", ex.getMessage()));
            }
        }
        return merged;
    }

    private String resolveNext(String current, GraphState state) {
        if (conditionalEdges.containsKey(current)) {
            String next = conditionalEdges.get(current).next(state);
            log.debug("[graph] conditional edge {} -> {}", current, next);
            return next;
        }
        return unconditionalEdges.getOrDefault(current, END);
    }

    private RunResult error(String threadId, String msg, GraphState state) {
        log.error("[graph:{}] ERROR: {}", threadId, msg);
        return new RunResult(RunResult.Status.ERROR, state, null, msg, threadId);
    }

    // -----------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final Map<String, GraphNode>     nodes              = new LinkedHashMap<>();
        private final Map<String, String>        unconditionalEdges = new LinkedHashMap<>();
        private final Map<String, EdgeCondition> conditionalEdges   = new LinkedHashMap<>();
        private final Map<String, List<String>>  parallelEdges      = new LinkedHashMap<>();
        private String          entryPoint;
        private int             maxSteps = 50;
        private ExecutorService executor;

        public Builder node(String name, GraphNode node) {
            nodes.put(name, node);
            return this;
        }

        /** Unconditional edge: after `from` always go to `to`. */
        public Builder edge(String from, String to) {
            unconditionalEdges.put(from, to);
            return this;
        }

        /** Conditional edge: after `from` call condition to pick next node. */
        public Builder conditionalEdge(String from, EdgeCondition condition) {
            conditionalEdges.put(from, condition);
            return this;
        }

        /**
         * Fan-out: execute all nodes in `parallelNodes` concurrently after `from`,
         * merge all their state updates, then continue via the normal edge/condition
         * from `from`.
         */
        public Builder parallelEdge(String from, String... parallelNodes) {
            parallelEdges.put(from, List.of(parallelNodes));
            return this;
        }

        public Builder entryPoint(String nodeName) {
            this.entryPoint = nodeName;
            return this;
        }

        public Builder maxSteps(int steps) {
            this.maxSteps = steps;
            return this;
        }

        public Builder executor(ExecutorService exec) {
            this.executor = exec;
            return this;
        }

        public GraphEngine build() {
            validateNoCycles();
            return new GraphEngine(this);
        }

        private void validateNoCycles() {
            // Simple DFS cycle check on unconditional edges only
            Set<String> visited = new HashSet<>();
            Set<String> stack   = new HashSet<>();
            for (String start : nodes.keySet()) {
                if (detectCycle(start, visited, stack)) {
                    throw new IllegalStateException(
                            "Unconditional edge cycle detected starting at: " + start);
                }
            }
        }

        private boolean detectCycle(String node, Set<String> visited, Set<String> stack) {
            if (stack.contains(node))   return true;
            if (visited.contains(node)) return false;
            visited.add(node);
            stack.add(node);
            String next = unconditionalEdges.get(node);
            if (next != null && !END.equals(next) && detectCycle(next, visited, stack)) return true;
            stack.remove(node);
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Internal value types
    // -----------------------------------------------------------------------

    public record SuspendedRun(GraphState state, String nextNode, List<String> trace) {}
}
