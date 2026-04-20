package com.masterclass.microservices.cache.redis;

/**
 * Returned when a semantically-similar cached answer is found.
 * similarityScore teaches the user how "close" the match was (0–1 scale).
 */
public record CacheHit(
        String response,
        String matchedQuestion,  // original question that was cached
        double similarityScore   // cosine similarity — expose to client so they see the match quality
) {}
