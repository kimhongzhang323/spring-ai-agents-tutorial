package com.masterclass.microservices.messaging.batch;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable job ticket created the moment a request enters the queue.
 * The jobId is the client's handle for polling results.
 */
public record BatchRequest(
        String jobId,
        String prompt,
        Instant submittedAt
) {
    public static BatchRequest of(String prompt) {
        return new BatchRequest(UUID.randomUUID().toString(), prompt, Instant.now());
    }
}
