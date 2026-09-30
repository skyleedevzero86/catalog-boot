package com.sleekydz86.catalog.adapter.outbound.extract;

import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import com.sleekydz86.catalog.domain.extract.model.ExtractCodeMappingSpec;
import com.sleekydz86.catalog.domain.extract.model.ExtractColumnSpec;
import com.sleekydz86.catalog.domain.extract.model.ExtractDatasetManifest;
import com.sleekydz86.catalog.domain.extract.model.ExtractDatasetStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ExtractManifestJsonMapper {

    private final ObjectMapper objectMapper;

    public ExtractManifestJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(ExtractDatasetManifest manifest) {
        try {
            return objectMapper.writeValueAsString(toDocument(manifest));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("매니페스트 직렬화에 실패했습니다.", exception);
        }
    }

    public ExtractDatasetManifest fromJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalStateException("저장된 매니페스트가 비어 있습니다.");
        }
        try {
            return fromDocument(objectMapper.readValue(json, ExtractManifestDocument.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("매니페스트 역직렬화에 실패했습니다.", exception);
        }
    }

    private static ExtractManifestDocument toDocument(ExtractDatasetManifest manifest) {
        return new ExtractManifestDocument(
                manifest.datasetId(),
                manifest.sourceConnectionId(),
                manifest.stagingConnectionId(),
                manifest.stagingVendor().name(),
                manifest.stagingSchema(),
                manifest.stagingTableName(),
                manifest.rawTableName(),
                manifest.columns(),
                manifest.physicalColumnNames(),
                manifest.codeMappings(),
                manifest.mappingTableNames(),
                manifest.deduplicated(),
                manifest.rowCount(),
                manifest.duplicateCount(),
                manifest.status().name(),
                manifest.exportFilePaths(),
                manifest.errorMessage(),
                manifest.preparedAt() == null ? null : manifest.preparedAt().toString(),
                manifest.exportedAt() == null ? null : manifest.exportedAt().toString()
        );
    }

    private static ExtractDatasetManifest fromDocument(ExtractManifestDocument document) {
        return new ExtractDatasetManifest(
                document.datasetId(),
                document.sourceConnectionId(),
                document.stagingConnectionId(),
                DatabaseVendor.valueOf(document.stagingVendor()),
                document.stagingSchema(),
                document.stagingTableName(),
                document.rawTableName(),
                document.columns(),
                document.physicalColumnNames(),
                document.codeMappings(),
                document.mappingTableNames(),
                document.deduplicated(),
                document.rowCount(),
                document.duplicateCount(),
                ExtractDatasetStatus.valueOf(document.status()),
                document.exportFilePaths() == null ? List.of() : document.exportFilePaths(),
                document.errorMessage(),
                document.preparedAt() == null ? null : Instant.parse(document.preparedAt()),
                document.exportedAt() == null ? null : Instant.parse(document.exportedAt())
        );
    }

    private record ExtractManifestDocument(
            String datasetId,
            String sourceConnectionId,
            String stagingConnectionId,
            String stagingVendor,
            String stagingSchema,
            String stagingTableName,
            String rawTableName,
            List<ExtractColumnSpec> columns,
            List<String> physicalColumnNames,
            List<ExtractCodeMappingSpec> codeMappings,
            List<String> mappingTableNames,
            boolean deduplicated,
            long rowCount,
            long duplicateCount,
            String status,
            List<String> exportFilePaths,
            String errorMessage,
            String preparedAt,
            String exportedAt
    ) {
    }
}
