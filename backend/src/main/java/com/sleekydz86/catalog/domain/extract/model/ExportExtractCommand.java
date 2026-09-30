package com.sleekydz86.catalog.domain.extract.model;

import java.util.List;

public record ExportExtractCommand(
        String datasetId,
        String outputPath,
        boolean includeHeader,
        String outputFormat,
        boolean singleFile,
        List<String> selectedColumnKeys,
        Integer maxRowsPerFile,
        String actorId
) {
    public ExportExtractCommand {
        if (selectedColumnKeys == null) {
            selectedColumnKeys = List.of();
        }
        if (outputFormat == null || outputFormat.isBlank()) {
            outputFormat = "csv";
        }
    }
}
