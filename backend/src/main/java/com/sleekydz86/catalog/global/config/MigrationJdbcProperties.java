package com.sleekydz86.catalog.global.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "com.sleekydz86.catalog.migration.jdbc")
public record MigrationJdbcProperties(
        int maxRetries,
        long retryDelayMs,
        int parallelTableWorkers,
        int poolMaxSize,
        int sourceFetchSize
) {
    public MigrationJdbcProperties {
        if (maxRetries <= 0) {
            maxRetries = 3;
        }
        if (retryDelayMs <= 0) {
            retryDelayMs = 1000L;
        }
        if (parallelTableWorkers <= 0) {
            parallelTableWorkers = 4;
        }
        if (poolMaxSize <= 0) {
            poolMaxSize = 3;
        }
        if (sourceFetchSize <= 0) {
            sourceFetchSize = 1000;
        }
    }
}
