package com.sleekydz86.catalog.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "com.sleekydz86.catalog.extract-worker")
public record ExtractWorkerProperties(
        boolean enabled,
        String baseUrl,
        String callbackBaseUrl
) {
    public ExtractWorkerProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8090";
        }
        if (callbackBaseUrl == null || callbackBaseUrl.isBlank()) {
            callbackBaseUrl = "http://localhost:8081";
        }
    }
}
