public package com.sleekydz86.catalog.domain.extract.model;

import java.util.List;

public record ResolvedPrepareExtractCommand(
        String datasetId,
        String sourceConnectionId,
        String stagingConnectionId,
        DatabaseEndpoint source,
        DatabaseEndpoint staging,
        String stagingSchema,
        String sourceSchema,
        String tableName,
        String generatedSql,
        List<ExtractColumnSpec> columns,
        List<ExtractCodeMappingSpec> codeMappings,
        boolean deduplicate,
        boolean replaceExisting,
        int fetchSize,
        String actorId
) {
}
