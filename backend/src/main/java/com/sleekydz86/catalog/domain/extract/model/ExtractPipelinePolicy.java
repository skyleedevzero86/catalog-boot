package com.sleekydz86.catalog.domain.extract.model;

import java.time.Duration;

public record ExtractPipelinePolicy(
        int fetchSize,
        boolean deduplicateDefault,
        boolean replaceExistingDefault,
        int maxRowsPerFile,
        String defaultStagingConnectionId,
        boolean clientGeneratedSqlEnabled,
        int queryMaxRows,
        Duration queryTimeout
) {
}