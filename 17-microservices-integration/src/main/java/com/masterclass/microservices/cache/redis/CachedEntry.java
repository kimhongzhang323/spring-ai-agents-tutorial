package com.masterclass.microservices.cache.redis;

import java.time.Instant;
import java.util.List;

/**
 * One slot in the semantic cache: the original question, its embedding vector,
 * and the LLM response we want to reuse for semantically-similar future questions.
 *
 * Stored as a Redis Hash so each field is inspectable with `HGETALL`.
 */
public record CachedEntry(
        String id,            // Redis key suffix: agent:semcache:{id}
        String question,      // original question text (human-readable)
        List<Double> embedding, // dense vector from EmbeddingModel
        String response,      // LLM answer to reuse on cache hit
        Instant cachedAt
) {}
