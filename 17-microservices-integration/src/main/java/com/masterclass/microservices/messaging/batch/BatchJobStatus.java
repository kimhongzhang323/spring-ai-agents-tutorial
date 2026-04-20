package com.masterclass.microservices.messaging.batch;

public enum BatchJobStatus {
    PENDING,     // in queue, not yet picked up
    PROCESSING,  // batch fired, waiting for LLM response
    DONE,        // result available
    FAILED       // permanent failure — check errorMessage
}
