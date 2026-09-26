package com.sleekydz86.catalog.global.application;

import org.springframework.stereotype.Service;

@Service
public class ExtractApplicationService {

    private final ExtractDatasetCommandService extractDatasetCommandService;

    public ExtractApplicationService(ExtractDatasetCommandService extractDatasetCommandService) {
        this.extractDatasetCommandService = extractDatasetCommandService;
    }

    public PrepareExtractResult prepare(PrepareExtractCommand command) {
        return extractDatasetCommandService.prepare(extractDatasetCommandService.resolvePrepare(
                command.datasetId(),
                command.sourceConnectionId(),
                command.stagingConnectionId(),
                command.sourceSchema(),
                command.tableName(),
                command.generatedSql(),
                command.columns(),
                command.codeMappings(),
                command.deduplicate(),
                command.replaceExisting(),
                command.fetchSize(),
                command.actorId()
        ));
    }

    public ExportExtractResult export(ExportExtractCommand command) {
        return extractDatasetCommandService.export(command);
    }

    public void cleanup(CleanupExtractCommand command) {
        extractDatasetCommandService.cleanup(command);
    }

    public ExtractDatasetManifest getManifest(String datasetId) {
        return extractDatasetCommandService.getManifest(datasetId);
    }
}
