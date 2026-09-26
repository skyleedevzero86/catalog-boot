package com.sleekydz86.catalog.domain.migration.model;

import java.util.List;

public record StartSyncMigrationCommand(
        String sourceConnectionId,
        String targetConnectionId,
        String sourceSchema,
        String targetSchema,
        String tableName,
        int batchSize,
        boolean dropExisting,
        String actorId
) {
}
