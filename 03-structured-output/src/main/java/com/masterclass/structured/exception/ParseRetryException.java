package com.masterclass.structured.exception;

/**
 * Thrown when BeanOutputConverter cannot parse the LLM's response into the target type.
 * Resilience4j retries on this exception type (configured in application.yml).
 * After max-attempts are exhausted, ExtractionService returns a meaningful error response.
 */
public class ParseRetryException extends RuntimeException {
    public ParseRetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
