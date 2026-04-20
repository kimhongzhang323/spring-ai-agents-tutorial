package com.masterclass.microservices.messaging.batch;

import java.time.Instant;

public record BatchJobResult(
        String jobId,
        BatchJobStatus status,
        String result,       // null while PENDING / PROCESSING
        String errorMessage, // null unless FAILED
        Instant completedAt  // null until terminal state
) {
    public static BatchJobResult pending(String jobId) {
        return new BatchJobResult(jobId, BatchJobStatus.PENDING, null, null, null);
    }

    public static BatchJobResult done(String jobId, String result) {
        return new BatchJobResult(jobId, BatchJobStatus.DONE, result, null, Instant.now());
    }

    public static BatchJobResult failed(String jobId, String errorMessage) {
        return new BatchJobResult(jobId, BatchJobStatus.FAILED, null, errorMessage, Instant.now());
    }
}
