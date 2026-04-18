package com.masterclass.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.memory")
public record MemoryProperties(int maxMessages, int ttlMinutes) {
    public MemoryProperties() { this(20, 60); }
}
