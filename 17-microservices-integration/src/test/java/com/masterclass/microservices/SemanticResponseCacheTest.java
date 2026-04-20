package com.masterclass.microservices;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterclass.microservices.cache.redis.CacheHit;
import com.masterclass.microservices.cache.redis.SemanticResponseCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SemanticResponseCacheTest {

    private SemanticResponseCache cache;
    private StringRedisTemplate redis;
    private EmbeddingModel embeddingModel;

    // "Java language" embedding — a fixed 4-dim vector for deterministic tests
    private static final float[] JAVA_VEC = {0.9f, 0.1f, 0.05f, 0.02f};
    // Very similar to JAVA_VEC (cosine ≈ 0.999) — represents a related question
    private static final float[] JAVA_SIMILAR_VEC = {0.91f, 0.10f, 0.04f, 0.02f};
    // Unrelated (cosine ≈ 0.1 against JAVA_VEC)
    private static final float[] PYTHON_VEC = {0.05f, 0.95f, 0.02f, 0.01f};

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        embeddingModel = mock(EmbeddingModel.class);

        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> setOps = mock(SetOperations.class);

        when(redis.opsForHash()).thenReturn(hashOps);
        when(redis.opsForSet()).thenReturn(setOps);

        // Seed one cached entry: id=abc123, question="What is Java?"
        when(setOps.members("agent:semcache:index")).thenReturn(Set.of("abc123"));
        when(hashOps.entries("agent:semcache:abc123")).thenReturn(Map.of(
                "question", "What is Java?",
                "embedding", "[0.9, 0.1, 0.05, 0.02]",
                "response", "Java is a compiled, object-oriented language.",
                "cachedAt", "2026-04-21T10:00:00Z"
        ));

        cache = new SemanticResponseCache(redis, embeddingModel, new ObjectMapper());
        ReflectionTestUtils.setField(cache, "similarityThreshold", 0.92);
        ReflectionTestUtils.setField(cache, "ttlMinutes", 60L);
    }

    @Test
    void relatedQuestionProducesCacheHit() {
        // "Can you explain the Java language?" embeds very close to the stored entry
        when(embeddingModel.embed(anyString())).thenReturn(JAVA_SIMILAR_VEC);

        Optional<CacheHit> result = cache.findSimilar("Can you explain the Java language?");

        assertThat(result).isPresent();
        assertThat(result.get().response()).isEqualTo("Java is a compiled, object-oriented language.");
        assertThat(result.get().similarityScore()).isGreaterThan(0.92);
        assertThat(result.get().matchedQuestion()).isEqualTo("What is Java?");
    }

    @Test
    void unrelatedQuestionProducesCacheMiss() {
        when(embeddingModel.embed(anyString())).thenReturn(PYTHON_VEC);

        Optional<CacheHit> result = cache.findSimilar("What is Python?");

        assertThat(result).isEmpty();
    }

    @Test
    void putStoresEmbeddingAndResponseInRedis() {
        when(embeddingModel.embed(anyString())).thenReturn(JAVA_VEC);

        String id = cache.put("What is Spring AI?", "Spring AI is a framework for building AI apps in Java.");

        assertThat(id).isNotBlank();
        verify(redis.opsForHash()).putAll(startsWith("agent:semcache:"), argThat(map ->
                ((Map<?, ?>) map).containsKey("embedding") &&
                ((Map<?, ?>) map).containsKey("response")
        ));
        verify(redis.opsForSet()).add(eq("agent:semcache:index"), anyString());
    }
}
