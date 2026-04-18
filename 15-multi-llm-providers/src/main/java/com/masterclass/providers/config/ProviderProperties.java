package com.masterclass.providers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.providers")
public record ProviderProperties(
        String defaultStrategy,
        String enabled
) {}
