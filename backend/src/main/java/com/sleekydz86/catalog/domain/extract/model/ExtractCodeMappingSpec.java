package com.sleekydz86.catalog.domain.extract.model;

public record ExtractCodeMappingSpec(
        String sourceColumnKey,
        String codeNameColumnKey,
        String codeNameLabel,
        String schemaName,
        String codeTableName,
        String codeColumnName,
        String codeNameColumnName
) {
}