package com.masterclass.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(int chunkSize, int chunkOverlap, int topK, double similarityThreshold) {
    public RagProperties() { this(800, 100, 5, 0.7); }
}
