package com.sleekydz86.catalog.domain.fileload.model;

import java.util.List;

public record ExportedTableData(
        String schemaName,
        String tableName,
        String sheetName,
        List<String> columnNames,
        List<List<String>> rows
) {
    public long rowCount() {
        return rows == null ? 0L : rows.size();
    }
}
