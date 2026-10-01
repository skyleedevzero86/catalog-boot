package com.sleekydz86.catalog.domain.migration.service;

import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.migration.model.LoadTableResult;
import com.sleekydz86.catalog.domain.migration.model.LoadTableToTargetCommand;
import com.sleekydz86.catalog.domain.migration.model.MigrationJob;
import com.sleekydz86.catalog.domain.migration.model.MigrationJobStatus;
import com.sleekydz86.catalog.domain.migration.model.MigrationJobTable;
import com.sleekydz86.catalog.domain.migration.model.MigrationTableStatus;
import com.sleekydz86.catalog.domain.migration.port.out.MigrationJobPersistencePort;
import com.sleekydz86.catalog.global.application.MigrationJobCancellationRegistry;
import com.sleekydz86.catalog.global.config.MigrationJdbcProperties;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class MigrationBatchCommandService {

    private final MigrationCommandService migrationCommandService;
    private final MigrationJobPersistencePort migrationJobPersistencePort;
    private final MigrationJdbcProperties migrationJdbcProperties;

    public MigrationBatchCommandService(
            MigrationCommandService migrationCommandService,
            MigrationJobPersistencePort migrationJobPersistencePort,
            MigrationJdbcProperties migrationJdbcProperties
    ) {
        this.migrationCommandService = migrationCommandService;
        this.migrationJobPersistencePort = migrationJobPersistencePort;
        this.migrationJdbcProperties = migrationJdbcProperties;
    }

    public void executeJob(String jobId, DatabaseEndpoint source, DatabaseEndpoint target, boolean failedOnly) {
        MigrationJob job = migrationJobPersistencePort.findJob(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("마이그레이션 작업을 찾을 수 없습니다: " + jobId));

        if (MigrationJobCancellationRegistry.isCancelled(jobId)) {
            migrationJobPersistencePort.markJobCancelled(jobId, "작업이 취소되었습니다.");
            return;
        }

        migrationJobPersistencePort.markJobRunning(jobId);
        List<MigrationJobTable> tables = migrationJobPersistencePort.findJobTables(jobId).stream()
                .filter(table -> shouldProcess(table, failedOnly))
                .toList();

        AtomicLong totalRows = new AtomicLong(0L);
        int workers = Math.max(1, Math.min(migrationJdbcProperties.parallelTableWorkers(), Math.max(1, tables.size())));
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (MigrationJobTable table : tables) {
                futures.add(executor.submit(() -> processTable(jobId, job, source, target, table, totalRows)));
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("마이그레이션 작업이 중단되었습니다.", exception);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    if (cause instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }
                    throw new IllegalStateException("마이그레이션 테이블 처리에 실패했습니다.", cause);
                }
            }
        } finally {
            executor.shutdownNow();
        }

        if (MigrationJobCancellationRegistry.isCancelled(jobId)) {
            migrationJobPersistencePort.markJobCancelled(jobId, "작업이 취소되었습니다.");
            MigrationJobCancellationRegistry.clear(jobId);
            return;
        }

        List<MigrationJobTable> finalTables = migrationJobPersistencePort.findJobTables(jobId);
        long success = finalTables.stream().filter(t -> t.status() == MigrationTableStatus.SUCCESS).count();
        long failed = finalTables.stream().filter(t -> t.status() == MigrationTableStatus.FAILED).count();
        MigrationJobStatus status;
        if (failed == 0) {
            status = MigrationJobStatus.SUCCESS;
        } else if (success == 0) {
            status = MigrationJobStatus.FAILED;
        } else {
            status = MigrationJobStatus.PARTIAL_SUCCESS;
        }
        migrationJobPersistencePort.markJobFinished(jobId, status, totalRows.get(), null);
        MigrationJobCancellationRegistry.clear(jobId);
    }

    private void processTable(
            String jobId,
            MigrationJob job,
            DatabaseEndpoint source,
            DatabaseEndpoint target,
            MigrationJobTable table,
            AtomicLong totalRows
    ) {
        if (MigrationJobCancellationRegistry.isCancelled(jobId)) {
            return;
        }
        migrationJobPersistencePort.markTableRunning(table.jobTableId());
        try {
            LoadTableResult result = migrationCommandService.handle(new LoadTableToTargetCommand(
                    source,
                    target,
                    job.sourceSchema(),
                    job.targetSchema(),
                    table.tableName(),
                    job.batchSize(),
                    job.dropExisting(),
                    jobId
            ));
            migrationJobPersistencePort.markTableSucceeded(table.jobTableId(), result);
            totalRows.addAndGet(result.rowsLoaded());
        } catch (RuntimeException exception) {
            migrationJobPersistencePort.markTableFailed(table.jobTableId(), exception.getMessage());
        }
    }

    private boolean shouldProcess(MigrationJobTable table, boolean failedOnly) {
        if (failedOnly) {
            return table.status() == MigrationTableStatus.FAILED || table.status() == MigrationTableStatus.PENDING;
        }
        return table.status() != MigrationTableStatus.SUCCESS;
    }
}
