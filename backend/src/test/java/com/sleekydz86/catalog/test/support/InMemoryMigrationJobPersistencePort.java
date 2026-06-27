package com.sleekydz86.catalog.test.support;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMigrationJobPersistencePort implements MigrationJobPersistencePort {

    private final Map<String, MigrationJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, List<MigrationJobTable>> tablesByJob = new ConcurrentHashMap<>();

    @Override
    public MigrationJob createJob(StartBatchMigrationCommand command, List<String> resolvedTableNames) {
        String jobId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        String actor = command.actorId() == null ? "system" : command.actorId();

        MigrationJob job = new MigrationJob(
                jobId,
                command.sourceConnectionId(),
                command.targetConnectionId(),
                command.mtdtId(),
                command.sourceSchema(),
                command.targetSchema(),
                command.batchSize(),
                command.dropExisting(),
                MigrationJobStatus.PENDING,
                resolvedTableNames.size(),
                0,
                0,
                0L,
                null,
                null,
                null,
                actor,
                now,
                now
        );
        jobs.put(jobId, job);

        List<MigrationJobTable> tables = new ArrayList<>();
        for (int i = 0; i < resolvedTableNames.size(); i++) {
            tables.add(new MigrationJobTable(
                    UUID.randomUUID().toString(),
                    jobId,
                    resolvedTableNames.get(i),
                    MigrationTableStatus.PENDING,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    i + 1
            ));
        }
        tablesByJob.put(jobId, tables);
        return job;
    }

    @Override
    public MigrationJob createSyncJob(StartSyncMigrationCommand command) {
        return createJob(new StartBatchMigrationCommand(
                command.sourceConnectionId(),
                command.targetConnectionId(),
                null,
                command.sourceSchema(),
                command.targetSchema(),
                List.of(command.tableName()),
                command.batchSize(),
                command.dropExisting(),
                command.actorId()
        ), List.of(command.tableName()));
    }

    @Override
    public void markJobCancelled(String jobId, String reason) {
        markJobFinished(jobId, MigrationJobStatus.CANCELLED, 0L, reason);
    }

    @Override
    public void resetFailedTables(String jobId) {
        List<MigrationJobTable> tables = new ArrayList<>();
        for (MigrationJobTable table : findJobTables(jobId)) {
            if (table.status() == MigrationTableStatus.FAILED) {
                tables.add(new MigrationJobTable(
                        table.jobTableId(), table.jobId(), table.tableName(), MigrationTableStatus.PENDING,
                        null, null, null, null, null, null, table.sortOrder()
                ));
            } else {
                tables.add(table);
            }
        }
        tablesByJob.put(jobId, tables);
        updateJob(jobId, job -> new MigrationJob(
                job.jobId(), job.sourceConnectionId(), job.targetConnectionId(), job.mtdtId(),
                job.sourceSchema(), job.targetSchema(), job.batchSize(), job.dropExisting(),
                MigrationJobStatus.PENDING, job.totalTableCount(), job.successTableCount(),
                job.failedTableCount(), job.totalRowCount(), null,
                null, null, job.creatorId(), job.createdAt(), Instant.now()
        ));
    }

    @Override
    public List<MigrationJob> findJobs(int limit) {
        return jobs.values().stream()
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .limit(limit)
                .toList();
    }

    @Override
    public void markJobRunning(String jobId) {
        updateJob(jobId, job -> new MigrationJob(
                job.jobId(), job.sourceConnectionId(), job.targetConnectionId(), job.mtdtId(),
                job.sourceSchema(), job.targetSchema(), job.batchSize(), job.dropExisting(),
                MigrationJobStatus.RUNNING, job.totalTableCount(), job.successTableCount(),
                job.failedTableCount(), job.totalRowCount(), job.errorMessage(),
                Instant.now(), job.endedAt(), job.creatorId(), job.createdAt(), Instant.now()
        ));
    }

    @Override
    public void markJobFinished(String jobId, MigrationJobStatus status, long totalRowCount, String errorMessage) {
        updateJob(jobId, job -> new MigrationJob(
                job.jobId(), job.sourceConnectionId(), job.targetConnectionId(), job.mtdtId(),
                job.sourceSchema(), job.targetSchema(), job.batchSize(), job.dropExisting(),
                status,
                job.totalTableCount(),
                countTables(jobId, MigrationTableStatus.SUCCESS),
                countTables(jobId, MigrationTableStatus.FAILED),
                totalRowCount,
                errorMessage,
                job.startedAt() == null ? Instant.now() : job.startedAt(),
                Instant.now(),
                job.creatorId(), job.createdAt(), Instant.now()
        ));
    }

    @Override
    public void markTableRunning(String jobTableId) {
        updateTable(jobTableId, table -> new MigrationJobTable(
                table.jobTableId(), table.jobId(), table.tableName(), MigrationTableStatus.RUNNING,
                table.rowCount(), table.batchCount(), table.createTableDdl(), table.errorMessage(),
                Instant.now(), table.endedAt(), table.sortOrder()
        ));
    }

    @Override
    public void markTableSucceeded(String jobTableId, LoadTableResult result) {
        updateTable(jobTableId, table -> new MigrationJobTable(
                table.jobTableId(), table.jobId(), table.tableName(), MigrationTableStatus.SUCCESS,
                result.rowsLoaded(), result.batchCount(), result.createTableDdl(), null,
                table.startedAt(), Instant.now(), table.sortOrder()
        ));
    }

    @Override
    public void markTableFailed(String jobTableId, String errorMessage) {
        updateTable(jobTableId, table -> new MigrationJobTable(
                table.jobTableId(), table.jobId(), table.tableName(), MigrationTableStatus.FAILED,
                table.rowCount(), table.batchCount(), table.createTableDdl(), errorMessage,
                table.startedAt(), Instant.now(), table.sortOrder()
        ));
    }

    @Override
    public Optional<MigrationJob> findJob(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    @Override
    public List<MigrationJobTable> findJobTables(String jobId) {
        return List.copyOf(tablesByJob.getOrDefault(jobId, List.of()));
    }

    private void updateJob(String jobId, java.util.function.Function<MigrationJob, MigrationJob> updater) {
        jobs.computeIfPresent(jobId, (id, job) -> updater.apply(job));
    }

    private void updateTable(String jobTableId, java.util.function.Function<MigrationJobTable, MigrationJobTable> updater) {
        for (Map.Entry<String, List<MigrationJobTable>> entry : tablesByJob.entrySet()) {
            List<MigrationJobTable> updated = new ArrayList<>();
            boolean changed = false;
            for (MigrationJobTable table : entry.getValue()) {
                if (table.jobTableId().equals(jobTableId)) {
                    updated.add(updater.apply(table));
                    changed = true;
                } else {
                    updated.add(table);
                }
            }
            if (changed) {
                tablesByJob.put(entry.getKey(), updated);
                return;
            }
        }
    }

    private int countTables(String jobId, MigrationTableStatus status) {
        return (int) findJobTables(jobId).stream().filter(t -> t.status() == status).count();
    }
}
