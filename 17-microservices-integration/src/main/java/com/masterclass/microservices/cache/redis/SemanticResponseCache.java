package com.masterclass.microservices.cache.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Semantic response cache backed by Redis.
 *
 * How it works (teach this in README):
 *
 *  WRITE path:
 *    question → EmbeddingModel → float[] vector
 *    Stored as a Redis Hash:  agent:semcache:{uuid}
 *      question  = original text
 *      embedding = JSON-serialised float array
 *      response  = LLM answer
 *      cachedAt  = ISO timestamp
 *    Entry key added to a Redis Set (the index): agent:semcache:index
 *
 *  READ path:
 *    incoming question → embed → compare cosine similarity against all index entries
 *    If best similarity ≥ threshold → cache HIT, return stored response
 *    Otherwise → cache MISS, caller must invoke the LLM
 *
 *  Why cosine similarity?
 *    "What is Java?" and "Can you explain the Java language?" have different tokens
 *    but nearly identical embedding vectors → cosine ≈ 0.97 → same cached answer reused.
 *    MD5/SHA hashing would miss this entirely.
 *
 *  Production note:
 *    This loads all embeddings into memory for comparison.
 *    At scale, replace with Redis Stack vector search (HNSW index) so comparison
 *    stays server-side and sub-millisecond at millions of entries.
 */
@Service
public class SemanticResponseCache {

    private static final Logger log = LoggerFactory.getLogger(SemanticResponseCache.class);
    private static final String INDEX_KEY = "agent:semcache:index";
    private static final String HASH_PREFIX = "agent:semcache:";

    @Value("${cache.semantic.similarity-threshold:0.92}")
    private double similarityThreshold;

    @Value("${cache.semantic.ttl-minutes:60}")
    private long ttlMinutes;

    private final StringRedisTemplate redis;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;

    public SemanticResponseCache(StringRedisTemplate redis,
                                  EmbeddingModel embeddingModel,
                                  ObjectMapper objectMapper) {
        this.redis = redis;
        this.embeddingModel = embeddingModel;
        this.objectMapper = objectMapper;
    }

    /**
     * Look up a semantically similar cached response.
     *
     * @return Optional with the cached answer, or empty on cache miss
     */
    public Optional<CacheHit> findSimilar(String question) {
        float[] queryVector = embed(question);

        Set<String> ids = redis.opsForSet().members(INDEX_KEY);
        if (ids == null || ids.isEmpty()) return Optional.empty();

        String bestId = null;
        double bestScore = -1;

        for (String id : ids) {
            Map<Object, Object> hash = redis.opsForHash().entries(HASH_PREFIX + id);
            if (hash.isEmpty()) continue;

            try {
                List<Double> stored = objectMapper.readValue(
                        (String) hash.get("embedding"),
                        new TypeReference<>() {});

                double score = cosineSimilarity(queryVector, stored);
                if (score > bestScore) {
                    bestScore = score;
                    bestId = id;
                }
            } catch (Exception e) {
                log.warn("Skipping corrupt cache entry id={}", id);
            }
        }

        if (bestId != null && bestScore >= similarityThreshold) {
            Map<Object, Object> hash = redis.opsForHash().entries(HASH_PREFIX + bestId);
            String cachedResponse = (String) hash.get("response");
            String originalQuestion = (String) hash.get("question");
            log.info("Semantic cache HIT similarity={} id={}", String.format("%.4f", bestScore), bestId);
            return Optional.of(new CacheHit(cachedResponse, originalQuestion, bestScore));
        }

        log.debug("Semantic cache MISS bestScore={} threshold={}", String.format("%.4f", bestScore), similarityThreshold);
        return Optional.empty();
    }

    /**
     * Store a question+response pair. Called after a real LLM invocation.
     */
    public String put(String question, String response) {
        float[] vector = embed(question);
        String id = UUID.randomUUID().toString();

        try {
            Map<String, String> hash = new HashMap<>();
            hash.put("question", question);
            hash.put("embedding", objectMapper.writeValueAsString(toDoubleList(vector)));
            hash.put("response", response);
            hash.put("cachedAt", Instant.now().toString());

            String hashKey = HASH_PREFIX + id;
            redis.opsForHash().putAll(hashKey, hash);
            redis.expire(hashKey, Duration.ofMinutes(ttlMinutes));
            redis.opsForSet().add(INDEX_KEY, id);

            log.info("Semantic cache PUT id={} question='{}'", id, abbreviated(question));
        } catch (Exception e) {
            log.error("Failed to write to semantic cache: {}", e.getMessage());
        }
        return id;
    }

    // ── internals ──────────────────────────────────────────────────────────

    private float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    /**
     * Cosine similarity: dot(a,b) / (|a| * |b|)
     * Range: -1 to 1. For text embeddings, expect 0.85–0.99 for related sentences.
     */
    private double cosineSimilarity(float[] a, List<Double> bList) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < Math.min(a.length, bList.size()); i++) {
            double bi = bList.get(i);
            dot += a[i] * bi;
            normA += (double) a[i] * a[i];
            normB += bi * bi;
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<Double> toDoubleList(float[] arr) {
        List<Double> list = new ArrayList<>(arr.length);
        for (float f : arr) list.add((double) f);
        return list;
    }

    private String abbreviated(String s) {
        return s.length() > 60 ? s.substring(0, 57) + "..." : s;
    }
}
