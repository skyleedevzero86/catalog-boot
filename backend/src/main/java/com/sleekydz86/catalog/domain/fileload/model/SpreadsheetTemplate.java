package com.sleekydz86.catalog.domain.fileload.model;

import java.util.List;

public record SpreadsheetTemplate(
        String fileName,
        String contentType,
        byte[] content,
        List<String> columnNames
) {
}
