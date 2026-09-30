package com.sleekydz86.catalog.domain.migration.port.out;

import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;

import java.util.List;

public interface TargetDatabasePort {

    void executeDdl(DatabaseEndpoint target, String ddl);

    void dropTableIfExists(DatabaseEndpoint target, String schemaName, String tableName);

    long batchInsert(
            DatabaseEndpoint target,
            String schemaName,
            String tableName,
            List<String> columnNames,
            List<List<Object>> rows
    );
}
