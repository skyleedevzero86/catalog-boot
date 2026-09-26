package com.sleekydz86.catalog.global.application;

import com.sleekydz86.catalog.adapter.outbound.persistence.metadata.MetaTableListRow;
import com.sleekydz86.catalog.domain.migration.model.MigrationJob;
import com.sleekydz86.catalog.domain.migration.model.MigrationJobStatus;
import com.sleekydz86.catalog.domain.migration.model.MigrationJobTable;
import com.sleekydz86.catalog.domain.migration.model.StartBatchMigrationCommand;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MigrationJobApplicationService {

    private final MigrationJobPersistencePort migrationJobPersistencePort;
    private final MigrationJobRunner migrationJobRunner;
    private final MetadataQueryService metadataQueryService;

    public MigrationJobApplicationService(
            MigrationJobPersistencePort migrationJobPersistencePort,
            MigrationJobRunner migrationJobRunner,
            MetadataQueryService metadataQueryService
    ) {
        this.migrationJobPersistencePort = migrationJobPersistencePort;
        this.migrationJobRunner = migrationJobRunner;
        this.metadataQueryService = metadataQueryService;
    }

    @Transactional
    public MigrationJob startBatchMigration(StartBatchMigrationCommand command) {
        List<String> tableNames = resolveTableNames(command);
        if (tableNames.isEmpty()) {
            throw new IllegalArgumentException("적재할 테이블이 없습니다. tableNames 또는 mtdtId를 지정하세요.");
        }

        MigrationJob job = migrationJobPersistencePort.createJob(command, tableNames);
        MigrationJobCancellationRegistry.register(job.jobId());
        migrationJobRunner.runAsync(job.jobId(), false);
        return job;
    }

    public List<MigrationJob> listJobs(int limit) {
        return migrationJobPersistencePort.findJobs(limit <= 0 ? 50 : limit);
    }

    public MigrationJob getJob(String jobId) {
        return migrationJobPersistencePort.findJob(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("마이그레이션 작업을 찾을 수 없습니다: " + jobId));
    }

    public List<MigrationJobTable> getJobTables(String jobId) {
        requireJob(jobId);
        return migrationJobPersistencePort.findJobTables(jobId);
    }

    @Transactional
    public MigrationJob cancelJob(String jobId) {
        MigrationJob job = getJob(jobId);
        if (job.status() != MigrationJobStatus.PENDING && job.status() != MigrationJobStatus.RUNNING) {
            throw new IllegalStateException("취소할 수 없는 작업 상태입니다: " + job.status());
        }
        MigrationJobCancellationRegistry.requestCancel(jobId);
        migrationJobPersistencePort.markJobCancelled(jobId, "사용자에 의해 취소됨");
        return getJob(jobId);
    }

    @Transactional
    public MigrationJob retryJob(String jobId, boolean failedOnly) {
        MigrationJob job = getJob(jobId);
        if (job.status() != MigrationJobStatus.FAILED
                && job.status() != MigrationJobStatus.PARTIAL_SUCCESS
                && job.status() != MigrationJobStatus.CANCELLED) {
            throw new IllegalStateException("재시도할 수 없는 작업 상태입니다: " + job.status());
        }
        migrationJobPersistencePort.resetFailedTables(jobId);
        MigrationJobCancellationRegistry.register(jobId);
        migrationJobRunner.runAsync(jobId, failedOnly);
        return getJob(jobId);
    }

    private MigrationJob requireJob(String jobId) {
        return migrationJobPersistencePort.findJob(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("마이그레이션 작업을 찾을 수 없습니다: " + jobId));
    }

    private List<String> resolveTableNames(StartBatchMigrationCommand command) {
        if (command.tableNames() != null && !command.tableNames().isEmpty()) {
            return command.tableNames();
        }
        if (command.mtdtId() == null || command.mtdtId().isBlank()) {
            return List.of();
        }
        List<MetaTableListRow> rows = metadataQueryService.listTables(command.mtdtId().trim());
        List<String> names = new ArrayList<>();
        for (MetaTableListRow row : rows) {
            if (row.getOrgnlTblNm() != null && !row.getOrgnlTblNm().isBlank()) {
                names.add(row.getOrgnlTblNm().trim());
            }
        }
        return names.stream().distinct().toList();
    }
}
