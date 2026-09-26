package com.sleekydz86.catalog.domain.migration.model;


import java.time.Instant;

public record MigrationJob(
        String jobId,
        String sourceConnectionId,
        String targetConnectionId,
        String mtdtId,
        String sourceSchema,
        String targetSchema,
        int batchSize,
        boolean dropExisting,
        MigrationJobStatus status,
        int totalTableCount,
        int successTableCount,
        int failedTableCount,
        long totalRowCount,
        String errorMessage,
        Instant startedAt,
        Instant endedAt,
        String creatorId,
        Instant createdAt,
        Instant modifiedAt
) {
}
