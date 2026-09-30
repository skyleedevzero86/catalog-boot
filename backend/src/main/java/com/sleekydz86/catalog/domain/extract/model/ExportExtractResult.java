package com.sleekydz86.catalog.domain.extract.model;

import java.util.List;

public record ExportExtractResult(
        String datasetId,
        ExtractDatasetStatus status,
        long rowCount,
        List<String> filePaths
) {
}
