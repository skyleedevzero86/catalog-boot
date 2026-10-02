package com.sleekydz86.catalog.domain.fileload.port.out;

import com.sleekydz86.catalog.domain.fileload.model.ExportedTableData;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetDbExport;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetFormat;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetTemplate;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

public interface SpreadsheetDocumentPort {

    SpreadsheetTemplate buildTemplate(String tableName, List<String> columnNames, SpreadsheetFormat format);

    List<List<String>> readDataRows(InputStream inputStream, SpreadsheetFormat format, List<String> expectedColumns);

    SpreadsheetDbExport buildDbExport(
            SpreadsheetFormat format,
            String connectionId,
            String extractedBy,
            Instant extractedAt,
            List<ExportedTableData> tables
    );
}
