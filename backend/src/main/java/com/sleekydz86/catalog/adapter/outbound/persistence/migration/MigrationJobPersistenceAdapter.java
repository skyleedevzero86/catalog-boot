package com.sleekydz86.catalog.adapter.outbound.persistence.migration;

import com.sleekydz86.catalog.domain.migration.model.*;
import com.sleekydz86.catalog.domain.migration.port.out.MigrationJobPersistencePort;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Transactional
public class MigrationJobPersistenceAdapter implements MigrationJobPersistencePort {

    private final MigrationJobCommandMapper commandMapper;

    public MigrationJobPersistenceAdapter(MigrationJobCommandMapper commandMapper) {
        this.commandMapper = commandMapper;
    }

    @Override
    public MigrationJob createJob(StartBatchMigrationCommand command, List<String> resolvedTableNames) {
        String actor = command.actorId() == null || command.actorId().isBlank() ? "system" : command.actorId().trim();

        Map<String, Object> jobParams = new HashMap<>();
        jobParams.put("op", "C");
        jobParams.put("migJobId", null);
        jobParams.put("srcLnkgId", command.sourceConnectionId());
        jobParams.put("trgtLnkgId", command.targetConnectionId());
        jobParams.put("mtdtId", trimToNull(command.mtdtId()));
        jobParams.put("srcSchmNm", trimToNull(command.sourceSchema()));
        jobParams.put("trgtSchmNm", command.targetSchema().trim());
        jobParams.put("batchSz", command.batchSize());
        jobParams.put("dropExstYn", command.dropExisting());
        jobParams.put("jobSttsCd", MigrationJobStatus.PENDING.name());
        jobParams.put("totTblCnt", resolvedTableNames.size());
        jobParams.put("actorId", actor);
        executeMigJob("C", jobParams);

        String jobId = (String) jobParams.get("migJobId");
        for (int i = 0; i < resolvedTableNames.size(); i++) {
            Map<String, Object> tableParams = new HashMap<>();
            tableParams.put("op", "C");
            tableParams.put("migJobTblId", null);
            tableParams.put("migJobId", jobId);
            tableParams.put("tblNm", resolvedTableNames.get(i));
            tableParams.put("tblSttsCd", MigrationTableStatus.PENDING.name());
            tableParams.put("sortNo", i + 1);
            executeMigJobTbl("C", tableParams);
        }

        return toJob(requireJobRow(jobId));
    }

    @Override
    public MigrationJob createSyncJob(StartSyncMigrationCommand command) {
        String actor = command.actorId() == null || command.actorId().isBlank() ? "system" : command.actorId().trim();
        Map<String, Object> jobParams = new HashMap<>();
        jobParams.put("op", "C");
        jobParams.put("migJobId", null);
        jobParams.put("srcLnkgId", command.sourceConnectionId());
        jobParams.put("trgtLnkgId", command.targetConnectionId());
        jobParams.put("srcSchmNm", trimToNull(command.sourceSchema()));
        jobParams.put("trgtSchmNm", command.targetSchema().trim());
        jobParams.put("batchSz", command.batchSize());
        jobParams.put("dropExstYn", command.dropExisting());
        jobParams.put("jobSttsCd", MigrationJobStatus.PENDING.name());
        jobParams.put("totTblCnt", 1);
        jobParams.put("actorId", actor);
        executeMigJob("C", jobParams);

        String jobId = (String) jobParams.get("migJobId");
        Map<String, Object> tableParams = new HashMap<>();
        tableParams.put("op", "C");
        tableParams.put("migJobTblId", null);
        tableParams.put("migJobId", jobId);
        tableParams.put("tblNm", command.tableName().trim());
        tableParams.put("tblSttsCd", MigrationTableStatus.PENDING.name());
        tableParams.put("sortNo", 1);
        executeMigJobTbl("C", tableParams);
        return toJob(requireJobRow(jobId));
    }

    @Override
    public void markJobCancelled(String jobId, String reason) {
        Map<String, Object> params = baseJobUpdate(jobId);
        params.put("jobSttsCd", MigrationJobStatus.CANCELLED.name());
        params.put("errMsgCn", trimToMax(reason, 4000));
        params.put("endDt", Instant.now());
        executeMigJob("U", params);
    }

    @Override
    public void resetFailedTables(String jobId) {
        commandMapper.resetFailedJobTables(jobId);
        Map<String, Object> params = baseJobUpdate(jobId);
        params.put("jobSttsCd", MigrationJobStatus.PENDING.name());
        params.put("errMsgCn", null);
        params.put("endDt", null);
        params.put("bgngDt", null);
        executeMigJob("U", params);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MigrationJob> findJobs(int limit) {
        return commandMapper.selectJobs(limit).stream().map(this::toJob).toList();
    }

    @Override
    public void markJobRunning(String jobId) {
        Map<String, Object> params = baseJobUpdate(jobId);
        params.put("jobSttsCd", MigrationJobStatus.RUNNING.name());
        params.put("bgngDt", Instant.now());
        executeMigJob("U", params);
    }

    @Override
    public void markJobFinished(String jobId, MigrationJobStatus status, long totalRowCount, String errorMessage) {
        List<MigrationJobTableRow> tables = commandMapper.selectJobTables(jobId);
        int successCount = (int) tables.stream().filter(t -> MigrationTableStatus.SUCCESS.name().equals(t.getTblSttsCd())).count();
        int failedCount = (int) tables.stream().filter(t -> MigrationTableStatus.FAILED.name().equals(t.getTblSttsCd())).count();

        Map<String, Object> params = baseJobUpdate(jobId);
        params.put("jobSttsCd", status.name());
        params.put("succTblCnt", successCount);
        params.put("failTblCnt", failedCount);
        params.put("totRowCnt", totalRowCount);
        params.put("errMsgCn", trimToMax(errorMessage, 4000));
        params.put("endDt", Instant.now());
        executeMigJob("U", params);
    }

    @Override
    public void markTableRunning(String jobTableId) {
        Map<String, Object> params = baseTableUpdate(jobTableId);
        params.put("tblSttsCd", MigrationTableStatus.RUNNING.name());
        params.put("bgngDt", Instant.now());
        executeMigJobTbl("U", params);
    }

    @Override
    public void markTableSucceeded(String jobTableId, LoadTableResult result) {
        Map<String, Object> params = baseTableUpdate(jobTableId);
        params.put("tblSttsCd", MigrationTableStatus.SUCCESS.name());
        params.put("rowCnt", result.rowsLoaded());
        params.put("batchCnt", result.batchCount());
        params.put("crtTblDdlCn", result.createTableDdl());
        params.put("errMsgCn", null);
        params.put("endDt", Instant.now());
        executeMigJobTbl("U", params);
    }

    @Override
    public void markTableFailed(String jobTableId, String errorMessage) {
        Map<String, Object> params = baseTableUpdate(jobTableId);
        params.put("tblSttsCd", MigrationTableStatus.FAILED.name());
        params.put("errMsgCn", trimToMax(errorMessage, 4000));
        params.put("endDt", Instant.now());
        executeMigJobTbl("U", params);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MigrationJob> findJob(String jobId) {
        return Optional.ofNullable(commandMapper.selectJobById(jobId)).map(this::toJob);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MigrationJobTable> findJobTables(String jobId) {
        return commandMapper.selectJobTables(jobId).stream().map(this::toJobTable).toList();
    }

    private MigrationJobRow requireJobRow(String jobId) {
        MigrationJobRow row = commandMapper.selectJobById(jobId);
        if (row == null) {
            throw new ResourceNotFoundException("마이그레이션 작업을 찾을 수 없습니다: " + jobId);
        }
        return row;
    }

    private Map<String, Object> baseJobUpdate(String jobId) {
        Map<String, Object> params = new HashMap<>();
        params.put("op", "U");
        params.put("migJobId", jobId);
        params.put("actorId", "system");
        return params;
    }

    private Map<String, Object> baseTableUpdate(String jobTableId) {
        Map<String, Object> params = new HashMap<>();
        params.put("op", "U");
        params.put("migJobTblId", jobTableId);
        return params;
    }

    private void executeMigJob(String op, Map<String, Object> params) {
        params.put("op", op);
        commandMapper.executeMigJob(params);
    }

    private void executeMigJobTbl(String op, Map<String, Object> params) {
        params.put("op", op);
        commandMapper.executeMigJobTbl(params);
    }

    private MigrationJob toJob(MigrationJobRow row) {
        return new MigrationJob(
                row.getMigJobId(), row.getSrcLnkgId(), row.getTrgtLnkgId(), row.getMtdtId(),
                row.getSrcSchmNm(), row.getTrgtSchmNm(), row.getBatchSz(), Boolean.TRUE.equals(row.getDropExstYn()),
                MigrationJobStatus.valueOf(row.getJobSttsCd()),
                row.getTotTblCnt(), row.getSuccTblCnt(), row.getFailTblCnt(), row.getTotRowCnt() == null ? 0L : row.getTotRowCnt(),
                row.getErrMsgCn(), row.getBgngDt(), row.getEndDt(), row.getCreatrId(), row.getCrtDt(), row.getMdfcnDt()
        );
    }

    private MigrationJobTable toJobTable(MigrationJobTableRow row) {
        return new MigrationJobTable(
                row.getMigJobTblId(), row.getMigJobId(), row.getTblNm(),
                MigrationTableStatus.valueOf(row.getTblSttsCd()),
                row.getRowCnt(), row.getBatchCnt(), row.getCrtTblDdlCn(), row.getErrMsgCn(),
                row.getBgngDt(), row.getEndDt(), row.getSortNo()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String trimToMax(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
