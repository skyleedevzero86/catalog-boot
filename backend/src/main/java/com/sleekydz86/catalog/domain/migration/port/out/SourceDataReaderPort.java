package com.sleekydz86.catalog.domain.migration.port.out;

import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import java.util.List;
import java.util.Map;

public interface SourceDataReaderPort {

    SourceTableBatchReader openTableReader(
            DatabaseEndpoint source,
            String schemaName,
            String tableName,
            List<String> columnNames,
            int batchSize
    );

    List<Map<String, Object>> readRows(
            DatabaseEndpoint source,
            String schemaName,
            String tableName,
            List<String> columnNames,
            int batchSize,
            int offset
    );
}
