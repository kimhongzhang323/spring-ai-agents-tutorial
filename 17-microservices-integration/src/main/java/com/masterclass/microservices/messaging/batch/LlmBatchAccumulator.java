package com.masterclass.microservices.messaging.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Core teaching component: shows WHY batching maximises LLM throughput.
 *
 * Design decisions (teach these in README):
 *  1. Two drain triggers — size threshold OR fixed time window (whichever fires first).
 *     Size-trigger keeps latency low for bursts; time-trigger flushes slow trickles.
 *  2. LinkedBlockingQueue is bounded so the app back-pressures callers
 *     instead of accumulating unbounded memory.
 *  3. Results live in a ConcurrentHashMap here; in production swap for Redis
 *     so multiple app instances share the result store.
 */
@Component
public class LlmBatchAccumulator {

    private static final Logger log = LoggerFactory.getLogger(LlmBatchAccumulator.class);

    @Value("${batch.max-size:5}")
    private int maxBatchSize;

    // Bounded queue — callers get 429 if this fills up (back-pressure)
    private final LinkedBlockingQueue<BatchRequest> queue = new LinkedBlockingQueue<>(100);

    // Shared result store — keyed by jobId
    final ConcurrentHashMap<String, BatchJobResult> results = new ConcurrentHashMap<>();

    private final LlmBatchProcessor processor;

    public LlmBatchAccumulator(LlmBatchProcessor processor) {
        this.processor = processor;
    }

    /**
     * Enqueue a request and immediately register it as PENDING so clients
     * can start polling before the batch fires.
     *
     * @throws IllegalStateException if the queue is full (back-pressure)
     */
    public BatchRequest enqueue(String prompt) {
        BatchRequest request = BatchRequest.of(prompt);
        if (!queue.offer(request)) {
            throw new IllegalStateException("Batch queue is full — try again later");
        }
        results.put(request.jobId(), BatchJobResult.pending(request.jobId()));
        log.debug("Enqueued jobId={} queueDepth={}", request.jobId(), queue.size());

        // Flush early if we've already hit the batch size ceiling
        if (queue.size() >= maxBatchSize) {
            drainAndProcess();
        }

        return request;
    }

    public BatchJobResult getResult(String jobId) {
        return results.getOrDefault(jobId,
                BatchJobResult.failed(jobId, "Unknown jobId — may have expired"));
    }

    /**
     * Time-window drain: fires every 500 ms regardless of batch size.
     * Ensures slow trickles are never stuck waiting forever.
     */
    @Scheduled(fixedDelayString = "${batch.drain-interval-ms:500}")
    void drainAndProcess() {
        if (queue.isEmpty()) return;

        List<BatchRequest> batch = new ArrayList<>(maxBatchSize);
        queue.drainTo(batch, maxBatchSize);

        if (batch.isEmpty()) return;

        log.info("Draining batch size={}", batch.size());

        // Mark all as PROCESSING before handing off — clients see progress immediately
        batch.forEach(r -> results.put(r.jobId(),
                new BatchJobResult(r.jobId(), BatchJobStatus.PROCESSING, null, null, null)));

        processor.processBatch(batch, results);
    }
}
