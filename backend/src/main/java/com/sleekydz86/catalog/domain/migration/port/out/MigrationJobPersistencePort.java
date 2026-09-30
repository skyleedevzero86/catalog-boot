package com.sleekydz86.catalog.domain.migration.port.out;

import com.sleekydz86.catalog.domain.migration.model.LoadTableResult;
import com.sleekydz86.catalog.domain.migration.model.MigrationJob;
import com.sleekydz86.catalog.domain.migration.model.MigrationJobStatus;
import com.sleekydz86.catalog.domain.migration.model.MigrationJobTable;
import com.sleekydz86.catalog.domain.migration.model.StartBatchMigrationCommand;
import com.sleekydz86.catalog.domain.migration.model.StartSyncMigrationCommand;

import java.util.List;
import java.util.Optional;

public interface MigrationJobPersistencePort {

    MigrationJob createJob(StartBatchMigrationCommand command, List<String> resolvedTableNames);

    MigrationJob createSyncJob(StartSyncMigrationCommand command);

    void markJobRunning(String jobId);

    void markJobFinished(String jobId, MigrationJobStatus status, long totalRowCount, String errorMessage);

    void markJobCancelled(String jobId, String reason);

    void resetFailedTables(String jobId);

    void markTableRunning(String jobTableId);

    void markTableSucceeded(String jobTableId, LoadTableResult result);

    void markTableFailed(String jobTableId, String errorMessage);

    Optional<MigrationJob> findJob(String jobId);

    List<MigrationJob> findJobs(int limit);

    List<MigrationJobTable> findJobTables(String jobId);
}
