package com.sleekydz86.catalog.domain.migration.model;

import java.time.Instant;

public record MigrationJobTable(
        String jobTableId,
        String jobId,
        String tableName,
        MigrationTableStatus status,
        Long rowCount,
        Integer batchCount,
        String createTableDdl,
        String errorMessage,
        Instant startedAt,
        Instant endedAt,
        int sortOrder
) {
}
