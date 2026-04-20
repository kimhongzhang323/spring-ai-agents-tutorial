package com.masterclass.microservices.messaging.batch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Two-endpoint pattern that teaches the submit-then-poll model:
 *
 *   POST /api/v1/batch/submit      → returns jobId immediately (non-blocking)
 *   GET  /api/v1/batch/result/{id} → client polls until status = DONE | FAILED
 *
 * This keeps the HTTP layer thin: no long-polling, no SSE complexity in module 17.
 * Streaming upgrades belong in the observability / streaming modules.
 */
@RestController
@RequestMapping("/api/v1/batch")
@Tag(name = "LLM Batch Queue", description = "Demonstrates queue-based batching to maximise LLM throughput")
public class BatchController {

    private final LlmBatchAccumulator accumulator;

    public BatchController(LlmBatchAccumulator accumulator) {
        this.accumulator = accumulator;
    }

    /**
     * Submit one or more prompts. Each gets its own jobId.
     * Returns immediately — LLM work happens in the background batch.
     *
     * Teaching point: the client never blocks waiting for the LLM.
     * The queue absorbs traffic spikes; the batch processor fills the
     * LLM's token-per-minute window efficiently.
     */
    @PostMapping("/submit")
    @Operation(summary = "Enqueue prompts for batch LLM processing",
               description = "Returns jobIds immediately. Poll /result/{jobId} for responses.")
    public ResponseEntity<List<Map<String, String>>> submit(@RequestBody List<String> prompts) {
        if (prompts == null || prompts.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<Map<String, String>> accepted = prompts.stream()
                    .map(prompt -> {
                        BatchRequest req = accumulator.enqueue(prompt);
                        return Map.of(
                                "jobId", req.jobId(),
                                "status", BatchJobStatus.PENDING.name(),
                                "pollUrl", "/api/v1/batch/result/" + req.jobId()
                        );
                    })
                    .toList();

            return ResponseEntity.accepted().body(accepted);

        } catch (IllegalStateException e) {
            // Queue full — apply back-pressure
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
    }

    /**
     * Poll for a single job result.
     * Status lifecycle: PENDING → PROCESSING → DONE | FAILED
     */
    @GetMapping("/result/{jobId}")
    @Operation(summary = "Poll for a batch job result")
    public ResponseEntity<BatchJobResult> result(@PathVariable String jobId) {
        BatchJobResult result = accumulator.getResult(jobId);
        HttpStatus status = switch (result.status()) {
            case DONE, FAILED -> HttpStatus.OK;
            case PENDING, PROCESSING -> HttpStatus.ACCEPTED; // 202 = "not ready yet, keep polling"
        };
        return ResponseEntity.status(status).body(result);
    }

    /**
     * Queue depth snapshot — useful for dashboards and teaching rate-limit reasoning.
     */
    @GetMapping("/stats")
    @Operation(summary = "Current queue depth and result store size")
    public Map<String, Integer> stats() {
        return Map.of(
                "resultStoreSize", accumulator.results.size()
        );
    }
}
