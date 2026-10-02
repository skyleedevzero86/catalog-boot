package com.sleekydz86.catalog.domain.extract.service;

import com.sleekydz86.catalog.domain.connection.port.out.ConnectionEndpointPort;
import com.sleekydz86.catalog.domain.extract.model.CleanupExtractCommand;
import com.sleekydz86.catalog.domain.extract.model.ExportExtractCommand;
import com.sleekydz86.catalog.domain.extract.model.ExportExtractResult;
import com.sleekydz86.catalog.domain.extract.model.ExtractCodeMappingSpec;
import com.sleekydz86.catalog.domain.extract.model.ExtractColumnSpec;
import com.sleekydz86.catalog.domain.extract.model.ExtractDatasetManifest;
import com.sleekydz86.catalog.domain.extract.model.ExtractDatasetStatus;
import com.sleekydz86.catalog.domain.extract.model.ExtractPipelinePolicy;
import com.sleekydz86.catalog.domain.extract.model.ExtractQueryCommand;
import com.sleekydz86.catalog.domain.extract.model.ValidatedExtractQuery;
import com.sleekydz86.catalog.domain.extract.model.PrepareExtractResult;
import com.sleekydz86.catalog.domain.extract.model.ResolvedPrepareExtractCommand;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractCodeMappingPort;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractDatasetStorePort;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractExportPort;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractStagingPort;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.global.exception.ErrorCode;
import com.sleekydz86.catalog.global.exception.InfrastructureException;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExtractDatasetCommandService {

    private static final Logger log = LoggerFactory.getLogger(ExtractDatasetCommandService.class);

    private final ExtractDatasetStorePort extractDatasetStorePort;
    private final ConnectionEndpointPort connectionEndpointPort;
    private final ExtractStagingPort extractStagingPort;
    private final ExtractCodeMappingPort extractCodeMappingPort;
    private final ExtractExportPort extractExportPort;
    private final ExtractPipelinePolicy extractPipelinePolicy;
    private final ExtractQueryService extractQueryService;

    public ExtractDatasetCommandService(
            ExtractDatasetStorePort extractDatasetStorePort,
            ConnectionEndpointPort connectionEndpointPort,
            ExtractStagingPort extractStagingPort,
            ExtractCodeMappingPort extractCodeMappingPort,
            ExtractExportPort extractExportPort,
            ExtractPipelinePolicy extractPipelinePolicy,
            ExtractQueryService extractQueryService
    ) {
        this.extractDatasetStorePort = extractDatasetStorePort;
        this.connectionEndpointPort = connectionEndpointPort;
        this.extractStagingPort = extractStagingPort;
        this.extractCodeMappingPort = extractCodeMappingPort;
        this.extractExportPort = extractExportPort;
        this.extractPipelinePolicy = extractPipelinePolicy;
        this.extractQueryService = extractQueryService;
    }

    public PrepareExtractResult prepare(ResolvedPrepareExtractCommand command) {
        String datasetId = command.datasetId();
        ExtractStagingPort.TableNames tableNames = extractStagingPort.tableNames(datasetId);
        String rawTable = tableNames.rawTable();
        String finalTable = tableNames.finalTable();

        List<String> physicalColumnNames = new ArrayList<>();
        List<String> sourceColumnKeys = new ArrayList<>();
        for (int i = 0; i < command.columns().size(); i++) {
            physicalColumnNames.add(extractStagingPort.physicalColumnName(i + 1));
            sourceColumnKeys.add(command.columns().get(i).key());
        }

        if (command.replaceExisting()) {
            extractStagingPort.dropTableIfExists(command.staging(), command.stagingSchema(), rawTable);
            extractStagingPort.dropTableIfExists(command.staging(), command.stagingSchema(), finalTable);
        }
        extractStagingPort.createRawStagingTable(command.staging(), command.stagingSchema(), rawTable, physicalColumnNames);

        ValidatedExtractQuery query = extractQueryService.build(
                new ExtractQueryCommand(
                        command.source().vendor(),
                        command.sourceSchema(),
                        command.tableName(),
                        command.generatedSql(),
                        sourceColumnKeys,
                        extractPipelinePolicy.clientGeneratedSqlEnabled()
                ),
                extractPipelinePolicy.queryMaxRows(),
                extractPipelinePolicy.queryTimeout()
        );
        long loaded = extractStagingPort.loadFromSql(
                command.source(),
                query,
                sourceColumnKeys,
                command.staging(),
                command.stagingSchema(),
                rawTable,
                physicalColumnNames,
                command.fetchSize()
        );

        long duplicateCount = 0;
        long rowCount = loaded;
        String activeTable = rawTable;
        if (command.deduplicate()) {
            ExtractStagingPort.DedupResult dedupResult = extractStagingPort.deduplicate(
                    command.staging(),
                    command.stagingSchema(),
                    rawTable,
                    finalTable
            );
            duplicateCount = dedupResult.duplicateCount();
            rowCount = dedupResult.finalCount();
            activeTable = finalTable;
            extractStagingPort.dropTableIfExists(command.staging(), command.stagingSchema(), rawTable);
        }

        List<String> mappingTables = new ArrayList<>();
        for (ExtractCodeMappingSpec mapping : command.codeMappings()) {
            String physical = physicalForKey(command.columns(), mapping.sourceColumnKey());
            mappingTables.add(extractCodeMappingPort.createAndPopulateMappingTable(
                    command.source(),
                    command.staging(),
                    command.stagingSchema(),
                    activeTable,
                    datasetId,
                    mapping,
                    physical
            ));
        }

        ExtractDatasetManifest manifest = new ExtractDatasetManifest(
                datasetId,
                command.sourceConnectionId(),
                command.stagingConnectionId(),
                command.staging().vendor(),
                command.stagingSchema(),
                activeTable,
                rawTable,
                command.columns(),
                physicalColumnNames,
                command.codeMappings(),
                mappingTables,
                command.deduplicate(),
                rowCount,
                duplicateCount,
                ExtractDatasetStatus.PREPARED,
                List.of(),
                null,
                Instant.now(),
                null
        );
        extractDatasetStorePort.save(manifest);
        return new PrepareExtractResult(
                datasetId,
                ExtractDatasetStatus.PREPARED,
                rowCount,
                duplicateCount,
                activeTable,
                mappingTables
        );
    }

    public ExportExtractResult export(ExportExtractCommand command) {
        ExtractDatasetManifest manifest = requireManifest(command.datasetId());
        if (manifest.status() != ExtractDatasetStatus.PREPARED && manifest.status() != ExtractDatasetStatus.COMPLETED) {
            throw new IllegalStateException("PREPARED 상태의 데이터셋만 export 할 수 있습니다: " + manifest.status());
        }
        DatabaseEndpoint staging = connectionEndpointPort.requireEndpoint(manifest.stagingConnectionId());
        extractDatasetStorePort.save(manifest.withStatus(ExtractDatasetStatus.EXPORTING, null));
        try {
            int maxRowsPerFile = command.maxRowsPerFile() == null || command.maxRowsPerFile() <= 0
                    ? extractPipelinePolicy.maxRowsPerFile()
                    : command.maxRowsPerFile();
            List<String> filePaths = extractExportPort.export(manifest, new ExtractExportPort.ExportRequest(
                    staging,
                    command.outputPath(),
                    command.includeHeader(),
                    command.outputFormat(),
                    command.singleFile(),
                    command.selectedColumnKeys(),
                    maxRowsPerFile
            ));
            ExtractDatasetManifest exported = manifest.withExport(filePaths, Instant.now());
            extractDatasetStorePort.save(exported);
            return new ExportExtractResult(command.datasetId(), ExtractDatasetStatus.COMPLETED, exported.rowCount(), filePaths);
        } catch (RuntimeException exception) {
            log.error("Extract export 실패 datasetId={}", command.datasetId(), exception);
            try {
                extractDatasetStorePort.save(manifest.withStatus(ExtractDatasetStatus.FAILED, exception.getMessage()));
            } catch (RuntimeException persistException) {
                log.error("Extract 실패 상태 저장 중 오류 datasetId={}", command.datasetId(), persistException);
            }
            if (exception instanceof InfrastructureException infrastructureException) {
                throw infrastructureException;
            }
            throw InfrastructureException.of(
                    ErrorCode.EXTRACT_FAILED,
                    "데이터셋 export에 실패했습니다: " + command.datasetId(),
                    exception
            );
        }
    }

    public void cleanup(CleanupExtractCommand command) {
        ExtractDatasetManifest manifest = requireManifest(command.datasetId());
        DatabaseEndpoint staging = connectionEndpointPort.requireEndpoint(manifest.stagingConnectionId());
        extractStagingPort.dropTableIfExists(staging, manifest.stagingSchema(), manifest.stagingTableName());
        if (manifest.rawTableName() != null) {
            extractStagingPort.dropTableIfExists(staging, manifest.stagingSchema(), manifest.rawTableName());
        }
        for (String mappingTable : manifest.mappingTableNames()) {
            extractStagingPort.dropTableIfExists(staging, manifest.stagingSchema(), mappingTable);
        }
        if (command.dropManifest()) {
            extractDatasetStorePort.delete(command.datasetId());
        } else {
            extractDatasetStorePort.save(manifest.withStatus(ExtractDatasetStatus.CLEANED, null));
        }
    }

    public ExtractDatasetManifest getManifest(String datasetId) {
        return requireManifest(datasetId);
    }

    public String resolveStagingConnectionId(String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        if (extractPipelinePolicy.defaultStagingConnectionId() == null
                || extractPipelinePolicy.defaultStagingConnectionId().isBlank()) {
            throw new IllegalArgumentException("stagingConnectionId 또는 cdw.catalog.extract.default-staging-connection-id 가 필요합니다.");
        }
        return extractPipelinePolicy.defaultStagingConnectionId().trim();
    }

    public String resolveSchema(DatabaseEndpoint staging) {
        if (staging.schemaName() != null && !staging.schemaName().isBlank()) {
            return staging.schemaName();
        }
        return switch (staging.vendor()) {
            case POSTGRESQL -> "public";
            case CLICKHOUSE -> staging.database();
            default -> staging.database();
        };
    }

    public String newDatasetId(String requested) {
        String datasetId = requested == null || requested.isBlank()
                ? UUID.randomUUID().toString()
                : requested.trim();
        if (datasetId.length() > 36) {
            throw new IllegalArgumentException("datasetId는 36자 이하여야 합니다.");
        }
        return datasetId;
    }

    public ResolvedPrepareExtractCommand resolvePrepare(
            String datasetId,
            String sourceConnectionId,
            String stagingConnectionId,
            String sourceSchema,
            String tableName,
            String generatedSql,
            List<ExtractColumnSpec> columns,
            List<ExtractCodeMappingSpec> codeMappings,
            Boolean deduplicate,
            Boolean replaceExisting,
            Integer fetchSize,
            String actorId
    ) {
        validatePrepare(columns, generatedSql, tableName);
        String resolvedDatasetId = newDatasetId(datasetId);
        String resolvedStagingConnectionId = resolveStagingConnectionId(stagingConnectionId);
        DatabaseEndpoint source = connectionEndpointPort.requireEndpoint(sourceConnectionId);
        DatabaseEndpoint staging = connectionEndpointPort.requireEndpoint(resolvedStagingConnectionId);
        return new ResolvedPrepareExtractCommand(
                resolvedDatasetId,
                sourceConnectionId,
                resolvedStagingConnectionId,
                source,
                staging,
                resolveSchema(staging),
                sourceSchema,
                tableName,
                generatedSql,
                columns,
                codeMappings == null ? List.of() : codeMappings,
                deduplicate == null ? extractPipelinePolicy.deduplicateDefault() : deduplicate,
                replaceExisting == null ? extractPipelinePolicy.replaceExistingDefault() : replaceExisting,
                fetchSize == null || fetchSize <= 0 ? extractPipelinePolicy.fetchSize() : fetchSize,
                actorId
        );
    }

    private ExtractDatasetManifest requireManifest(String datasetId) {
        return extractDatasetStorePort.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("추출 데이터셋을 찾을 수 없습니다: " + datasetId));
    }

    private void validatePrepare(List<ExtractColumnSpec> columns, String generatedSql, String tableName) {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("columns는 최소 1개 이상이어야 합니다.");
        }
        boolean hasSql = generatedSql != null && !generatedSql.isBlank();
        boolean hasTable = tableName != null && !tableName.isBlank();
        if (!hasSql && !hasTable) {
            throw new IllegalArgumentException("generatedSql 또는 tableName 중 하나는 필수입니다.");
        }
    }

    private String physicalForKey(List<ExtractColumnSpec> columns, String key) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).key().equals(key)) {
                return extractStagingPort.physicalColumnName(i + 1);
            }
        }
        throw new IllegalArgumentException("코드매핑 대상 컬럼을 찾을 수 없습니다: " + key);
    }
}
