package com.sleekydz86.catalog.global.config;

import com.sleekydz86.catalog.domain.connection.port.ConnectionPersistencePort;
import com.sleekydz86.catalog.domain.connection.port.ConnectionTestPort;
import com.sleekydz86.catalog.domain.connection.port.SecretCipherPort;
import com.sleekydz86.catalog.domain.connection.service.ConnectionCommandService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            cdw.catalog.domain.category.port.out.CategoryPersistencePort categoryPersistencePort,
            cdw.catalog.domain.metadata.port.out.MetaPersistencePort metaPersistencePort
    ) {
        return new CategoryCommandService(categoryPersistencePort, metaPersistencePort);
    }

    @Bean
    MetaSyncService metaSyncService(
            cdw.catalog.domain.metadata.port.out.MetaPersistencePort metaPersistencePort,
            ConnectionPersistencePort connectionPersistencePort,
            SourceMetadataPort sourceMetadataPort,
            SecretCipherPort secretCipherPort
    ) {
        return new MetaSyncService(metaPersistencePort, connectionPersistencePort, sourceMetadataPort, secretCipherPort);
    }

    @Bean
    ExtractRequestCommandService extractRequestCommandService(
            cdw.catalog.domain.extract.port.out.ExtractWorkerPort extractWorkerPort
    ) {
        return new ExtractRequestCommandService(extractWorkerPort);
    }
}
