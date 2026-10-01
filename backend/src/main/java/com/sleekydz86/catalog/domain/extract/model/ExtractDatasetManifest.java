package com.sleekydz86.catalog.domain.extract.model;

import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ExtractDatasetManifest(
        String datasetId,
        String sourceConnectionId,
        String stagingConnectionId,
        DatabaseVendor stagingVendor,
        String stagingSchema,
        String stagingTableName,
        String rawTableName,
        List<ExtractColumnSpec> columns,
        List<String> physicalColumnNames,
        List<ExtractCodeMappingSpec> codeMappings,
        List<String> mappingTableNames,
        boolean deduplicated,
        long rowCount,
        long duplicateCount,
        ExtractDatasetStatus status,
        List<String> exportFilePaths,
        String errorMessage,
        Instant preparedAt,
        Instant exportedAt
) {
    public ExtractDatasetManifest withStatus(ExtractDatasetStatus status, String errorMessage) {
        return new ExtractDatasetManifest(
                datasetId, sourceConnectionId, stagingConnectionId, stagingVendor, stagingSchema,
                stagingTableName, rawTableName, columns, physicalColumnNames, codeMappings, mappingTableNames,
                deduplicated, rowCount, duplicateCount, status, exportFilePaths, errorMessage,
                preparedAt, exportedAt
        );
    }

    public ExtractDatasetManifest withExport(List<String> filePaths, Instant exportedAt) {
        return new ExtractDatasetManifest(
                datasetId, sourceConnectionId, stagingConnectionId, stagingVendor, stagingSchema,
                stagingTableName, rawTableName, columns, physicalColumnNames, codeMappings, mappingTableNames,
                deduplicated, rowCount, duplicateCount, ExtractDatasetStatus.COMPLETED, filePaths, errorMessage,
                preparedAt, exportedAt
        );
    }
}
