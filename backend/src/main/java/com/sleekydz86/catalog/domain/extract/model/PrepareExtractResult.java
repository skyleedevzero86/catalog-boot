package com.sleekydz86.catalog.domain.extract.model;

import java.util.List;

public record PrepareExtractResult(
        String datasetId,
        ExtractDatasetStatus status,
        long rowCount,
        long duplicateCount,
        String stagingTableName,
        List<String> mappingTableNames
) {
}