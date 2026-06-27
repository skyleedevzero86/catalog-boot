package com.sleekydz86.catalog.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "com.sleekydz86.catalog.extract-worker")
public record ExtractWorkerProperties(
        boolean enabled,
        String baseUrl,
        String callbackBaseUrl
) {
}
