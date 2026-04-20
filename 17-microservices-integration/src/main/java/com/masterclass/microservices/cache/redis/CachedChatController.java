package com.masterclass.microservices.cache.redis;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Demonstrates the cache-aside pattern for LLM responses:
 *
 *   1. Embed incoming question
 *   2. Check Redis for a semantically-similar past answer (cosine similarity ≥ 0.92)
 *   3a. HIT  → return cached response immediately, skip the LLM entirely
 *   3b. MISS → call LLM, store {embedding, response} in Redis, return to client
 *
 * Teaching clues in the response:
 *   X-Cache: HIT | MISS   — did we skip the LLM?
 *   X-Cache-Score: 0.974  — how similar was the matched question?
 *   X-Cache-Matched: ...  — what original question matched?
 *
 * Watch the logs: cache HITs show no LLM call; latency drops from ~800ms to ~20ms.
 */
@RestController
@RequestMapping("/api/v1/cache")
@Tag(name = "Semantic Cache", description = "Redis semantic response cache — skip the LLM for related questions")
public class CachedChatController {

    private final SemanticResponseCache cache;
    private final ChatClient chatClient;

    public CachedChatController(SemanticResponseCache cache, ChatClient.Builder builder) {
        this.cache = cache;
        this.chatClient = builder.build();
    }

    /**
     * Ask a question. The first time, the LLM answers and the response is cached.
     * Subsequent semantically-similar questions are served directly from Redis.
     *
     * Try asking:
     *   "What is Java?"                         → MISS (LLM called, result cached)
     *   "Can you explain the Java language?"    → HIT  (similarity ≈ 0.95)
     *   "Tell me about Java programming"        → HIT  (similarity ≈ 0.93)
     *   "What is Python?"                       → MISS (unrelated, LLM called again)
     */
    @PostMapping("/chat")
    @Operation(
        summary = "Chat with semantic Redis cache",
        description = "Returns cached LLM response for semantically similar questions. Check X-Cache header."
    )
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "question is required"));
        }

        // ── Cache lookup ───────────────────────────────────────────────────
        Optional<CacheHit> hit = cache.findSimilar(question);
        if (hit.isPresent()) {
            CacheHit h = hit.get();
            return ResponseEntity.ok()
                    .header("X-Cache", "HIT")
                    .header("X-Cache-Score", String.format("%.4f", h.similarityScore()))
                    .header("X-Cache-Matched", h.matchedQuestion())
                    .body(Map.of(
                            "answer", h.response(),
                            "cacheStatus", "HIT",
                            "similarityScore", h.similarityScore(),
                            "matchedQuestion", h.matchedQuestion()
                    ));
        }

        // ── Cache miss: call LLM, then store result ────────────────────────
        String response = chatClient.prompt()
                .system("You are a helpful assistant. Answer concisely in 2–3 sentences.")
                .user(question)
                .call()
                .content();

        String cacheId = cache.put(question, response);

        return ResponseEntity.ok()
                .header("X-Cache", "MISS")
                .header("X-Cache-Id", cacheId)
                .body(Map.of(
                        "answer", response,
                        "cacheStatus", "MISS",
                        "cacheId", cacheId
                ));
    }

    /**
     * Inspect the raw Redis index size — useful during demos to watch the cache grow.
     */
    @GetMapping("/stats")
    @Operation(summary = "Cache entry count")
    public Map<String, Object> stats() {
        return Map.of("description",
                "Run SMEMBERS agent:semcache:index in redis-cli to inspect cache entries");
    }
}
