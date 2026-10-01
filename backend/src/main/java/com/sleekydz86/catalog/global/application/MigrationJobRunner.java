package com.sleekydz86.catalog.global.application;

import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionPersistencePort;
import com.sleekydz86.catalog.domain.connection.port.out.SecretCipherPort;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.migration.model.MigrationJob;
import com.sleekydz86.catalog.domain.migration.model.MigrationJobStatus;
import com.sleekydz86.catalog.domain.migration.port.out.MigrationJobPersistencePort;
import com.sleekydz86.catalog.domain.migration.service.MigrationBatchCommandService;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MigrationJobRunner {

    private final MigrationBatchCommandService migrationBatchCommandService;
    private final MigrationJobPersistencePort migrationJobPersistencePort;
    private final ConnectionPersistencePort connectionPersistencePort;
    private final SecretCipherPort secretCipherPort;

    public MigrationJobRunner(
            MigrationBatchCommandService migrationBatchCommandService,
            MigrationJobPersistencePort migrationJobPersistencePort,
            ConnectionPersistencePort connectionPersistencePort,
            SecretCipherPort secretCipherPort
    ) {
        this.migrationBatchCommandService = migrationBatchCommandService;
        this.migrationJobPersistencePort = migrationJobPersistencePort;
        this.connectionPersistencePort = connectionPersistencePort;
        this.secretCipherPort = secretCipherPort;
    }

    @Async("migrationJobExecutor")
    public void runAsync(String jobId) {
        runAsync(jobId, false);
    }

    @Async("migrationJobExecutor")
    public void runAsync(String jobId, boolean failedOnly) {
        try {
            MigrationJob job = migrationJobPersistencePort.findJob(jobId)
                    .orElseThrow(() -> new ResourceNotFoundException("마이그레이션 작업을 찾을 수 없습니다: " + jobId));

            if (MigrationJobCancellationRegistry.isCancelled(jobId)) {
                migrationJobPersistencePort.markJobCancelled(jobId, "작업이 취소되었습니다.");
                return;
            }

            DatabaseEndpoint source = toEndpoint(requireConnection(job.sourceConnectionId()));
            DatabaseEndpoint target = toEndpoint(requireConnection(job.targetConnectionId()));
            migrationBatchCommandService.executeJob(jobId, source, target, failedOnly);
        } catch (RuntimeException exception) {
            migrationJobPersistencePort.findJob(jobId).ifPresent(job -> {
                if (job.status() == MigrationJobStatus.PENDING || job.status() == MigrationJobStatus.RUNNING) {
                    migrationJobPersistencePort.markJobFinished(
                            jobId,
                            MigrationJobStatus.FAILED,
                            0L,
                            exception.getMessage()
                    );
                }
            });
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
