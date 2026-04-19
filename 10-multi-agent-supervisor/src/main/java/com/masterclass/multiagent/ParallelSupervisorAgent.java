package com.masterclass.multiagent;

import com.masterclass.multiagent.agent.AnalysisAgent;
import com.masterclass.multiagent.agent.ResearchAgent;
import com.masterclass.multiagent.agent.WriterAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Advanced multi-agent pattern: parallel sub-agent execution.
 *
 * <h2>The problem with sequential orchestration</h2>
 * The {@link SupervisorAgent} calls research → analyse → write sequentially.
 * Each LLM call takes 1–5 seconds. Three sequential calls = 3–15 seconds total.
 *
 * <h2>When can we parallelize?</h2>
 * Only when sub-agents don't depend on each other's output. Example: for a
 * competitor analysis report, you could research Company A and Company B in parallel,
 * then merge the results before writing.
 *
 * <h2>Pattern: fan-out + merge</h2>
 * <pre>
 *   User Request → Supervisor
 *     ├── [async] researchAgent.research("Company A")
 *     ├── [async] researchAgent.research("Company B")
 *     └── wait for both → writerAgent.write(mergedResult)
 * </pre>
 *
 * <h2>When NOT to parallelize</h2>
 * When sub-agents depend on each other's output (e.g., analysis needs research results),
 * parallelism is impossible. The sequential supervisor pattern is the correct choice.
 *
 * <h2>Implementation note</h2>
 * In Spring AI, each {@code chatClient.prompt()...call()} is a blocking call.
 * Use a virtual-thread executor (Java 21) or {@code WebClient} for true async.
 * This implementation uses {@link CompletableFuture} with a virtual-thread executor.
 */
@Service
public class ParallelSupervisorAgent {

    private static final Logger log = LoggerFactory.getLogger(ParallelSupervisorAgent.class);

    private final ResearchAgent researchAgent;
    private final WriterAgent writerAgent;
    private final ChatClient synthesisClient;

    // Java 21 virtual threads — cheap and ideal for I/O-bound LLM calls
    private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();

    public ParallelSupervisorAgent(ResearchAgent researchAgent,
                                   WriterAgent writerAgent,
                                   ChatClient.Builder builder) {
        this.researchAgent = researchAgent;
        this.writerAgent = writerAgent;
        this.synthesisClient = builder
                .defaultSystem("""
                        You are a synthesis agent. You receive research summaries from multiple
                        sources and merge them into a single coherent briefing. Preserve all key facts.
                        """)
                .build();
    }

    /**
     * Researches multiple topics in parallel, then synthesizes the results.
     * Total latency ≈ max(individual latencies) instead of sum.
     *
     * @param topics  the topics to research in parallel
     * @param request the synthesis instruction (e.g., "compare and contrast")
     */
    public ParallelResult processParallel(java.util.List<String> topics, String request) {
        log.info("Parallel research for {} topics: {}", topics.size(), topics);
        long start = System.currentTimeMillis();

        // Fan-out: kick off one research task per topic
        java.util.List<CompletableFuture<String>> researchFutures = topics.stream()
                .map(topic -> CompletableFuture.supplyAsync(
                        () -> {
                            log.debug("Researching topic: {}", topic);
                            return researchAgent.research(topic);
                        },
                        executor))
                .toList();

        // Wait for all research tasks to complete (fan-in)
        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                researchFutures.toArray(new CompletableFuture[0]));

        allDone.join(); // blocks until all parallel tasks complete

        java.util.List<String> researchResults = researchFutures.stream()
                .map(CompletableFuture::join)
                .toList();

        long researchMs = System.currentTimeMillis() - start;
        log.info("Parallel research completed in {}ms for {} topics", researchMs, topics.size());

        // Merge: synthesize all research into a single briefing
        String mergedContext = buildMergedContext(topics, researchResults);
        String synthesis = synthesisClient.prompt()
                .user(request + "\n\nResearch summaries:\n" + mergedContext)
                .call()
                .content();

        long totalMs = System.currentTimeMillis() - start;
        return new ParallelResult(synthesis, topics.size(), researchMs, totalMs);
    }

    private String buildMergedContext(java.util.List<String> topics,
                                      java.util.List<String> results) {
        var sb = new StringBuilder();
        for (int i = 0; i < topics.size(); i++) {
            sb.append("## ").append(topics.get(i)).append("\n");
            sb.append(results.get(i)).append("\n\n");
        }
        return sb.toString();
    }

    public record ParallelResult(
            String synthesis,
            int topicsResearched,
            long parallelResearchMs,
            long totalLatencyMs) {}
}
