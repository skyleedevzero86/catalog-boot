package com.sleekydz86.catalog.global.config;

import com.sleekydz86.catalog.domain.category.port.out.CategoryPersistencePort;
import com.sleekydz86.catalog.domain.category.service.CategoryCommandService;
import com.sleekydz86.catalog.domain.connection.port.out.*;
import com.sleekydz86.catalog.domain.connection.service.ConnectionCommandService;
import com.sleekydz86.catalog.domain.metadata.port.out.MetaPersistencePort;
import com.sleekydz86.catalog.domain.metadata.service.MetaSyncService;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractCodeMappingPort;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractDatasetStorePort;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractExportPort;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractStagingPort;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractWorkerPort;
import com.sleekydz86.catalog.domain.extract.model.ExtractPipelinePolicy;
import com.sleekydz86.catalog.domain.extract.service.DefaultExtractQueryService;
import com.sleekydz86.catalog.domain.extract.service.ExtractDatasetCommandService;
import com.sleekydz86.catalog.domain.extract.service.ExtractQueryService;
import com.sleekydz86.catalog.domain.extract.service.ExtractRequestCommandService;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionEndpointPort;
import com.sleekydz86.catalog.domain.fileload.port.out.FileLoadTargetPort;
import com.sleekydz86.catalog.domain.fileload.port.out.SpreadsheetDocumentPort;
import com.sleekydz86.catalog.domain.fileload.service.FileLoadCommandService;
import com.sleekydz86.catalog.domain.migration.port.out.DdlTypeMapperPort;
import com.sleekydz86.catalog.domain.migration.port.out.MigrationJobPersistencePort;
import com.sleekydz86.catalog.domain.migration.port.out.SourceDataReaderPort;
import com.sleekydz86.catalog.domain.migration.port.out.SourceMetadataPort;
import com.sleekydz86.catalog.domain.migration.port.out.TargetDatabasePort;
import com.sleekydz86.catalog.domain.migration.port.out.TargetDdlGeneratorPort;
import com.sleekydz86.catalog.domain.migration.service.DdlTypeMapper;
import com.sleekydz86.catalog.domain.migration.service.MigrationBatchCommandService;
import com.sleekydz86.catalog.domain.migration.service.MigrationCommandService;
import com.sleekydz86.catalog.domain.migration.service.TargetDdlGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class DomainServiceConfig {

    @Bean
    ConnectionCommandService connectionCommandService(
            ConnectionPersistencePort connectionPersistencePort,
            SecretCipherPort secretCipherPort,
            ConnectionTestPort connectionTestPort
    ) {
        return new ConnectionCommandService(connectionPersistencePort, secretCipherPort, connectionTestPort);
    }

    @Bean
    DdlTypeMapperPort ddlTypeMapperPort() {
        return new DdlTypeMapper();
    }

    @Bean
    TargetDdlGeneratorPort targetDdlGeneratorPort(DdlTypeMapperPort ddlTypeMapperPort) {
        return new TargetDdlGenerator(ddlTypeMapperPort);
    }

    @Bean
    MigrationCommandService migrationCommandService(
            SourceMetadataPort sourceMetadataPort,
            SourceDataReaderPort sourceDataReaderPort,
            TargetDatabasePort targetDatabasePort,
            TargetDdlGeneratorPort targetDdlGeneratorPort
    ) {
        return new MigrationCommandService(
                sourceMetadataPort,
                sourceDataReaderPort,
                targetDatabasePort,
                targetDdlGeneratorPort
        );
    }

    @Bean
    MigrationBatchCommandService migrationBatchCommandService(
            MigrationCommandService migrationCommandService,
            MigrationJobPersistencePort migrationJobPersistencePort,
            MigrationJdbcProperties migrationJdbcProperties
    ) {
        return new MigrationBatchCommandService(migrationCommandService, migrationJobPersistencePort, migrationJdbcProperties);
    }

    @Bean
    CategoryCommandService categoryCommandService(
            CategoryPersistencePort categoryPersistencePort,
            MetaPersistencePort metaPersistencePort
    ) {
        return new CategoryCommandService(categoryPersistencePort, metaPersistencePort);
    }

    @Bean
    MetaSyncService metaSyncService(
            MetaPersistencePort metaPersistencePort,
            ConnectionPersistencePort connectionPersistencePort,
            SourceMetadataPort sourceMetadataPort,
            SecretCipherPort secretCipherPort
    ) {
        return new MetaSyncService(metaPersistencePort, connectionPersistencePort, sourceMetadataPort, secretCipherPort);
    }

    @Bean
    ExtractRequestCommandService extractRequestCommandService(
            ExtractWorkerPort extractWorkerPort
    ) {
        return new ExtractRequestCommandService(extractWorkerPort);
    }

    @Bean
    ExtractPipelinePolicy extractPipelinePolicy(ExtractModuleProperties extractModuleProperties) {
        return new ExtractPipelinePolicy(
                extractModuleProperties.fetchSize(),
                extractModuleProperties.deduplicateDefault(),
                extractModuleProperties.replaceExistingDefault(),
                extractModuleProperties.maxRowsPerFile(),
                extractModuleProperties.defaultStagingConnectionId(),
                true,
                100_000,
                Duration.ofMinutes(5)
        );
    }

    @Bean
    ExtractQueryService extractQueryService() {
        return new DefaultExtractQueryService();
    }

    @Bean
    ExtractDatasetCommandService extractDatasetCommandService(
            ExtractDatasetStorePort extractDatasetStorePort,
            ConnectionEndpointPort connectionEndpointPort,
            ExtractStagingPort extractStagingPort,
            ExtractCodeMappingPort extractCodeMappingPort,
            ExtractExportPort extractExportPort,
            ExtractPipelinePolicy extractPipelinePolicy,
            ExtractQueryService extractQueryService
    ) {
        return new ExtractDatasetCommandService(
                extractDatasetStorePort,
                connectionEndpointPort,
                extractStagingPort,
                extractCodeMappingPort,
                extractExportPort,
                extractPipelinePolicy,
                extractQueryService
        );
    }

    @Bean
    FileLoadCommandService fileLoadCommandService(
            ConnectionPersistencePort connectionPersistencePort,
            ConnectionTestPort connectionTestPort,
            SecretCipherPort secretCipherPort,
            SourceMetadataPort sourceMetadataPort,
            FileLoadTargetPort fileLoadTargetPort,
            SpreadsheetDocumentPort spreadsheetDocumentPort
    ) {
        return new FileLoadCommandService(
                connectionPersistencePort,
                connectionTestPort,
                secretCipherPort,
                sourceMetadataPort,
                fileLoadTargetPort,
                spreadsheetDocumentPort
        );
    }
}
