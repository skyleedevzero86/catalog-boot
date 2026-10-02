package com.sleekydz86.catalog.domain.fileload.port.out;

import com.sleekydz86.catalog.domain.fileload.model.FileColumnDef;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;

import java.util.List;

public interface FileLoadTargetPort {

    void createTable(
            DatabaseEndpoint target,
            String schemaName,
            String tableName,
            String tableComment,
            List<FileColumnDef> columns
    );

    long insertRows(
            DatabaseEndpoint target,
            String schemaName,
            String tableName,
            List<String> columnNames,
            List<List<Object>> rows
    );
}
