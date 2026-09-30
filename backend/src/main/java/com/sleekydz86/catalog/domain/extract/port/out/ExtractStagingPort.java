package com.sleekydz86.catalog.domain.extract.port.out;

import com.sleekydz86.catalog.domain.connection.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.extract.model.ValidatedExtractQuery;
import java.util.List;
import java.util.Map;

public interface ExtractStagingPort {

    record TableNames(String rawTable, String finalTable) {
    }

    record DedupResult(long rawCount, long finalCount, long duplicateCount) {
    }

    TableNames tableNames(String datasetId);

    String physicalColumnName(int index);

    void dropTableIfExists(DatabaseEndpoint staging, String schemaName, String tableName);

    void createRawStagingTable(
            DatabaseEndpoint staging,
            String schemaName,
            String tableName,
            List<String> physicalColumnNames
    );

    long loadFromTable(
            DatabaseEndpoint source,
            String sourceSchema,
            String tableName,
            List<String> sourceColumnKeys,
            DatabaseEndpoint staging,
            String stagingSchema,
            String stagingTableName,
            List<String> physicalColumnNames,
            int fetchSize
    );

    long loadFromSql(
            DatabaseEndpoint source,
            ValidatedExtractQuery query,
            List<String> sourceColumnKeys,
            DatabaseEndpoint staging,
            String stagingSchema,
            String stagingTableName,
            List<String> physicalColumnNames,
            int fetchSize
    );

    DedupResult deduplicate(
            DatabaseEndpoint staging,
            String schemaName,
            String rawTableName,
            String finalTableName
    );
}
