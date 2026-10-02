package com.sleekydz86.catalog.global.application;

import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionPersistencePort;
import com.sleekydz86.catalog.domain.connection.port.out.SecretCipherPort;
import com.sleekydz86.catalog.domain.migration.model.*;
import com.sleekydz86.catalog.domain.migration.port.out.MigrationJobPersistencePort;
import com.sleekydz86.catalog.domain.migration.service.MigrationCommandService;
import com.sleekydz86.catalog.global.exception.ErrorCode;
import com.sleekydz86.catalog.global.exception.InfrastructureException;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MigrationApplicationService {

    private static final Logger log = LoggerFactory.getLogger(MigrationApplicationService.class);

    private final MigrationCommandService migrationCommandService;
    private final ConnectionPersistencePort connectionPersistencePort;
    private final SecretCipherPort secretCipherPort;
    private final MigrationJobPersistencePort migrationJobPersistencePort;

    public MigrationApplicationService(
            MigrationCommandService migrationCommandService,
            ConnectionPersistencePort connectionPersistencePort,
            SecretCipherPort secretCipherPort,
            MigrationJobPersistencePort migrationJobPersistencePort
    ) {
        this.migrationCommandService = migrationCommandService;
        this.connectionPersistencePort = connectionPersistencePort;
        this.secretCipherPort = secretCipherPort;
        this.migrationJobPersistencePort = migrationJobPersistencePort;
    }

    public TargetDdlPreview previewDdl(
            String sourceConnectionId,
            String targetConnectionId,
            String sourceSchema,
            String targetSchema,
            String tableName
    ) {
        DatabaseEndpoint source = toEndpoint(requireConnection(sourceConnectionId));
        DatabaseEndpoint target = toEndpoint(requireConnection(targetConnectionId));
        return migrationCommandService.handle(new PreviewTargetDdlCommand(
                source,
                target.vendor(),
                sourceSchema,
                targetSchema,
                tableName
        ));
    }

    @Transactional
    public LoadTableResult loadTable(
            String sourceConnectionId,
            String targetConnectionId,
            String sourceSchema,
            String targetSchema,
            String tableName,
            int batchSize,
            boolean dropExisting,
            String actorId
    ) {
        var job = migrationJobPersistencePort.createSyncJob(new StartSyncMigrationCommand(
                sourceConnectionId,
                targetConnectionId,
                sourceSchema,
                targetSchema,
                tableName,
                batchSize,
                dropExisting,
                actorId
        ));
        MigrationJobCancellationRegistry.register(job.jobId());
        migrationJobPersistencePort.markJobRunning(job.jobId());
        var tables = migrationJobPersistencePort.findJobTables(job.jobId());
        String jobTableId = tables.get(0).jobTableId();
        migrationJobPersistencePort.markTableRunning(jobTableId);

        DatabaseEndpoint source = toEndpoint(requireConnection(sourceConnectionId));
        DatabaseEndpoint target = toEndpoint(requireConnection(targetConnectionId));
        try {
            LoadTableResult result = migrationCommandService.handle(new LoadTableToTargetCommand(
                    source,
                    target,
                    sourceSchema,
                    targetSchema,
                    tableName,
                    batchSize,
                    dropExisting
            ));
            migrationJobPersistencePort.markTableSucceeded(jobTableId, result);
            migrationJobPersistencePort.markJobFinished(job.jobId(), MigrationJobStatus.SUCCESS, result.rowsLoaded(), null);
            return result;
        } catch (RuntimeException exception) {
            log.error(
                    "단일 테이블 적재 실패 jobId={} tableName={} cause={}",
                    job.jobId(),
                    tableName,
                    exception.getMessage(),
                    exception
            );
            String message = exception.getMessage() == null ? "알 수 없는 오류" : exception.getMessage();
            migrationJobPersistencePort.markTableFailed(jobTableId, message);
            migrationJobPersistencePort.markJobFinished(job.jobId(), MigrationJobStatus.FAILED, 0L, message);
            if (exception instanceof InfrastructureException infrastructureException) {
                throw infrastructureException;
            }
            throw InfrastructureException.of(
                    ErrorCode.MIGRATION_FAILED,
                    "테이블 적재에 실패했습니다: " + tableName,
                    exception
            );
        } finally {
            MigrationJobCancellationRegistry.clear(job.jobId());
        }
    }

    private ConnectionProfile requireConnection(String connectionId) {
        return connectionPersistencePort.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("연결을 찾을 수 없습니다: " + connectionId));
    }

    private DatabaseEndpoint toEndpoint(ConnectionProfile profile) {
        return new DatabaseEndpoint(
                profile.vendor(),
                profile.host(),
                profile.port(),
                profile.databaseName(),
                profile.schemaName(),
                profile.username(),
                secretCipherPort.decrypt(profile.encryptedPassword())
        );
    }
}
