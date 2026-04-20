package com.masterclass.microservices.messaging.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Fires every request in a batch concurrently using a virtual-thread executor.
 *
 * Teaching points:
 *  - CompletableFuture fan-out: all N prompts hit the LLM in parallel so the
 *    batch completes in ~max(individual latency), not sum(individual latency).
 *  - Virtual threads (Java 21) — cheap to spin up one per LLM call; no thread pool sizing needed.
 *  - Results written back into the shared map so the controller can serve polls.
 */
@Component
public class LlmBatchProcessor {

    private static final Logger log = LoggerFactory.getLogger(LlmBatchProcessor.class);

    // Virtual-thread executor: one lightweight thread per concurrent LLM call
    private static final Executor VIRTUAL = Executors.newVirtualThreadPerTaskExecutor();

    private final ChatClient chatClient;

    public LlmBatchProcessor(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * Launches one CompletableFuture per request, waits for all to finish,
     * then writes DONE or FAILED into the result map.
     */
    public void processBatch(List<BatchRequest> batch,
                             ConcurrentHashMap<String, BatchJobResult> results) {

        List<CompletableFuture<Void>> futures = batch.stream()
                .map(req -> CompletableFuture
                        .supplyAsync(() -> callLlm(req), VIRTUAL)
                        .thenAccept(response -> {
                            results.put(req.jobId(), BatchJobResult.done(req.jobId(), response));
                            log.debug("Completed jobId={}", req.jobId());
                        })
                        .exceptionally(ex -> {
                            results.put(req.jobId(), BatchJobResult.failed(req.jobId(), ex.getMessage()));
                            log.warn("Failed jobId={} reason={}", req.jobId(), ex.getMessage());
                            return null;
                        }))
                .toList();

        // Non-blocking join — drainAndProcess returns immediately; results arrive async
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("Batch of {} completed", batch.size()));
    }

    private String callLlm(BatchRequest req) {
        return chatClient.prompt()
                .system("You are a helpful assistant. Answer concisely.")
                .user(req.prompt())
                .call()
                .content();
    }
}
