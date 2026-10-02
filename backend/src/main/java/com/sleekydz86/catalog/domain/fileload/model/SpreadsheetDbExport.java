package com.sleekydz86.catalog.domain.fileload.model;

import java.time.Instant;
import java.util.List;

public record SpreadsheetDbExport(
        String fileName,
        String contentType,
        byte[] content,
        Instant extractedAt,
        String extractedBy,
        String connectionId,
        List<ExportedTableData> tables,
        long totalRowCount
) {
}
