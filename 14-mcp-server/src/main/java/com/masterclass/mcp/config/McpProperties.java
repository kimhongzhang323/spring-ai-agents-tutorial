package com.masterclass.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mcp")
public record McpProperties(
        Filesystem filesystem,
        Api api
) {
    public record Filesystem(String allowedBasePath) {}
    public record Api(int timeoutSeconds) {}
}
