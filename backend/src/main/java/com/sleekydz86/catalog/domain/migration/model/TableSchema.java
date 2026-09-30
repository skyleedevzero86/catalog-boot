package com.sleekydz86.catalog.domain.migration.model;

import java.util.List;

public record TableSchema(
        String schemaName,
        String tableName,
        List<ColumnSchema> columns
) {
}
