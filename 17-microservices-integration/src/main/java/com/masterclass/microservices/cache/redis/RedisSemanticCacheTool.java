package com.masterclass.microservices.cache.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.time.Duration;
import java.util.Optional;

/**
 * Wraps Redis as a semantic LLM response cache.
 * Identical prompts (same SHA-256 hash) return the cached response
 * without a new LLM call — cutting latency and cost for repeated queries.
 */
@Component
public class RedisSemanticCacheTool {

    private static final Logger log = LoggerFactory.getLogger(RedisSemanticCacheTool.class);
    private static final String PREFIX = "agent:cache:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    public RedisSemanticCacheTool(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Tool(description = """
            Looks up a cached LLM response for a given prompt key.
            Use this BEFORE calling the LLM: if a cached response exists, return it directly
            to avoid redundant API costs. The cache key is derived from the prompt's SHA-256 hash.
            Input: prompt (the exact user prompt to look up).
            Returns: the cached response string, or 'CACHE_MISS' if not found.
            """)
    public String getCachedResponse(String prompt) {
        String key = PREFIX + DigestUtils.md5DigestAsHex(prompt.getBytes());
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("Redis cache HIT for prompt hash");
            return cached;
        }
        log.debug("Redis cache MISS for prompt hash");
        return "CACHE_MISS";
    }

    @Tool(description = """
            Stores an LLM response in Redis cache with a 30-minute TTL.
            Call this AFTER receiving an LLM response to populate the cache for future
            identical requests. This dramatically reduces cost for high-traffic agents
            that receive many repeated questions.
            Input: prompt (the original user prompt), response (the LLM's response to cache).
            Returns: confirmation with the cache key and expiry time.
            """)
    public String cacheResponse(String prompt, String response) {
        String key = PREFIX + DigestUtils.md5DigestAsHex(prompt.getBytes());
        redisTemplate.opsForValue().set(key, response, TTL);
        log.debug("Redis cache SET: key={} ttl={}min", key, TTL.toMinutes());
        return "Response cached for 30 minutes. Key: " + key;
    }
}
