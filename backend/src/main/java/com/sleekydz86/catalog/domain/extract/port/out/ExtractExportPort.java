package com.sleekydz86.catalog.domain.extract.port.out;

import java.util.List;
import com.sleekydz86.catalog.domain.connection.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.extract.model.ExtractDatasetManifest;

public interface ExtractExportPort {

    record ExportRequest(
            DatabaseEndpoint stagingEndpoint,
            String outputPath,
            boolean includeHeader,
            String outputFormat,
            boolean singleFile,
            List<String> selectedColumnKeys,
            int maxRowsPerFile
    ) {
    }

    List<String> export(ExtractDatasetManifest manifest, ExportRequest request);
}