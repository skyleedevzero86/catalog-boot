package com.sleekydz86.catalog.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "com.sleekydz86.catalog.extract")
public record ExtractModuleProperties(
        String exportRoot,
        int fetchSize,
        boolean deduplicateDefault,
        boolean replaceExistingDefault,
        int maxRowsPerFile,
        String defaultStagingConnectionId
) {
    public ExtractModuleProperties {
        if (exportRoot == null || exportRoot.isBlank()) {
            exportRoot = "exports";
        }
        if (fetchSize <= 0) {
            fetchSize = 1000;
        }
        if (maxRowsPerFile <= 0) {
            maxRowsPerFile = 100_000;
        }
    }
}
