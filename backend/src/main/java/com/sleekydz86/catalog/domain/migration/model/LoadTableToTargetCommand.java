package com.sleekydz86.catalog.domain.migration.model;


public record LoadTableToTargetCommand(
        DatabaseEndpoint source,
        DatabaseEndpoint target,
        String sourceSchema,
        String targetSchema,
        String tableName,
        int batchSize,
        boolean dropExisting,
        String cancelJobId
) {
    public LoadTableToTargetCommand(
            DatabaseEndpoint source,
            DatabaseEndpoint target,
            String sourceSchema,
            String targetSchema,
            String tableName,
            int batchSize,
            boolean dropExisting
    ) {
        this(source, target, sourceSchema, targetSchema, tableName, batchSize, dropExisting, null);
    }

    public LoadTableToTargetCommand {
        if (batchSize < 1) {
            batchSize = 500;
        }
    }
}
