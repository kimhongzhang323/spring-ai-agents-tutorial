package com.masterclass.shared.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 1: in-process per-user Bucket4j buckets.
 * Phase 3 (module 07): replace with Redis-backed ProxyManager for multi-instance correctness.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties props;
    private final MeterRegistry meterRegistry;
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties props, MeterRegistry meterRegistry) {
        this.props = props;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }

        String userId = auth.getName();
        Bucket bucket = userBuckets.computeIfAbsent(userId, this::newBucket);

        if (bucket.tryConsume(1)) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            chain.doFilter(request, response);
        } else {
            meterRegistry.counter("rate_limit.exceeded", "user", userId).increment();
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setHeader("Content-Type", "application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"retryAfterSeconds\":60}");
        }
    }

    private Bucket newBucket(String userId) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(props.capacityPerUser())
                .refillGreedy(props.refillTokensPerMinute(), Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
