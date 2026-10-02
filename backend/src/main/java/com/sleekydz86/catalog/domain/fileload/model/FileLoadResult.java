package com.sleekydz86.catalog.domain.fileload.model;

public record FileLoadResult(
        String connectionId,
        String schemaName,
        String tableName,
        long insertedRows,
        String message
) {
}
