package com.sleekydz86.catalog.domain.migration.model;

import java.util.List;

public record StartBatchMigrationCommand(
        String sourceConnectionId,
        String targetConnectionId,
        String mtdtId,
        String sourceSchema,
        String targetSchema,
        List<String> tableNames,
        int batchSize,
        boolean dropExisting,
        String actorId
) {
    public StartBatchMigrationCommand {
        if (batchSize < 1) {
            batchSize = 500;
        }
        if (tableNames != null) {
            tableNames = tableNames.stream()
                    .map(String::trim)
                    .filter(name -> !name.isBlank())
                    .distinct()
                    .toList();
        }
    }
}
