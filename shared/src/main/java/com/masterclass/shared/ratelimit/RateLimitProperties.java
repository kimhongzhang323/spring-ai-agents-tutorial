package com.masterclass.shared.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        long capacityPerUser,
        long refillTokensPerMinute,
        long globalCapacity,
        long globalRefillTokensPerMinute
) {
    public RateLimitProperties() {
        this(20, 20, 200, 200);
    }
}
