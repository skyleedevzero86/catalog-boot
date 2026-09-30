package com.sleekydz86.catalog.domain.extract.model;

import java.util.List;

public record PrepareExtractCommand(
        String datasetId,
        String sourceConnectionId,
        String stagingConnectionId,
        String sourceSchema,
        String tableName,
        String generatedSql,
        List<ExtractColumnSpec> columns,
        List<ExtractCodeMappingSpec> codeMappings,
        Boolean deduplicate,
        Boolean replaceExisting,
        Integer fetchSize,
        String actorId
) {
    public PrepareExtractCommand {
        if (columns == null) {
            columns = List.of();
        }
        if (codeMappings == null) {
            codeMappings = List.of();
        }
    }
}
