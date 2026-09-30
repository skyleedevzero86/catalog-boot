package com.sleekydz86.catalog.adapter.outbound.persistence.extract;

import com.sleekydz86.catalog.adapter.outbound.extract.ExtractManifestJsonMapper;
import com.sleekydz86.catalog.domain.extract.model.ExtractDatasetManifest;
import com.sleekydz86.catalog.domain.extract.model.ExtractDatasetStatus;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractDatasetStorePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Transactional
@ConditionalOnProperty(prefix = "com.sleekydz86.catalog.extract", name = "store", havingValue = "database", matchIfMissing = true)
public class ExtractDatasetPersistenceAdapter implements ExtractDatasetStorePort {

    private static final String ACTOR = "system";

    private final ExtractDatasetCommandMapper commandMapper;
    private final ExtractManifestJsonMapper manifestJsonMapper;

    public ExtractDatasetPersistenceAdapter(
            ExtractDatasetCommandMapper commandMapper,
            ExtractManifestJsonMapper manifestJsonMapper
    ) {
        this.commandMapper = commandMapper;
        this.manifestJsonMapper = manifestJsonMapper;
    }

    @Override
    public void save(ExtractDatasetManifest manifest) {
        String json = manifestJsonMapper.toJson(manifest);
        String dbStatus = toDbStatus(manifest.status());
        Optional<ExtractDatasetRow> existing = commandMapper.selectDatstById(manifest.datasetId());
        if (existing.isEmpty()) {
            String dmndId = createDmnd(manifest.datasetId(), dbStatus);
            createDatst(manifest.datasetId(), dmndId, dbStatus, json);
            insertExcn(
                    manifest.datasetId(),
                    "PREPARE",
                    "SUCCESS",
                    manifest.rowCount(),
                    "TABLE",
                    manifest.stagingTableName(),
                    null,
                    null
            );
            return;
        }

        ExtractDatasetStatus previous = manifestJsonMapper.fromJson(existing.get().mnfstCn()).status();
        updateDatst(manifest.datasetId(), dbStatus, json);
        updateDmnd(existing.get().extrDmndId(), dbStatus);
        if (previous != manifest.status()) {
            recordStatusTransition(manifest);
        }
    }

    @Override
    public Optional<ExtractDatasetManifest> findById(String datasetId) {
        return commandMapper.selectDatstById(datasetId)
                .map(row -> manifestJsonMapper.fromJson(row.mnfstCn()));
    }

    @Override
    public void delete(String datasetId) {
        Optional<ExtractDatasetRow> row = commandMapper.selectDatstById(datasetId);
        if (row.isEmpty()) {
            return;
        }
        executeDatst("D", datasetId, row.get().extrDmndId(), null, null, null, null);
        executeDmnd("D", row.get().extrDmndId(), null);
    }

    private void recordStatusTransition(ExtractDatasetManifest manifest) {
        switch (manifest.status()) {
            case EXPORTING -> insertExcn(
                    manifest.datasetId(), "EXPORT", "RUNNING", null, null, null, null, null
            );
            case COMPLETED -> insertExcn(
                    manifest.datasetId(),
                    "EXPORT",
                    "SUCCESS",
                    manifest.rowCount(),
                    resolveFileStorageType(manifest),
                    null,
                    joinPaths(manifest.exportFilePaths()),
                    null
            );
            case FAILED -> insertExcn(
                    manifest.datasetId(),
                    "EXPORT",
                    "FAILED",
                    null,
                    null,
                    null,
                    null,
                    truncate(manifest.errorMessage())
            );
            case CLEANED -> insertExcn(
                    manifest.datasetId(),
                    "CLEANUP",
                    "SUCCESS",
                    null,
                    null,
                    null,
                    null,
                    null
            );
            default -> {
            }
        }
    }

    private String createDmnd(String datasetId, String dbStatus) {
        Map<String, Object> params = new HashMap<>();
        params.put("op", "C");
        params.put("extrDmndId", null);
        params.put("dmndSrcCd", "EXTERNAL_API");
        params.put("extrDmndNm", "Extract " + datasetId);
        params.put("extrDmndSttsCd", dbStatus);
        params.put("datstCnt", 1);
        params.put("actorId", ACTOR);
        commandMapper.executeExtrDmnd(params);
        return (String) params.get("extrDmndId");
    }

    private void updateDmnd(String dmndId, String dbStatus) {
        Map<String, Object> params = new HashMap<>();
        params.put("op", "U");
        params.put("extrDmndId", dmndId);
        params.put("extrDmndSttsCd", dbStatus);
        params.put("actorId", ACTOR);
        commandMapper.executeExtrDmnd(params);
    }

    private void createDatst(String datasetId, String dmndId, String dbStatus, String json) {
        Map<String, Object> params = new HashMap<>();
        params.put("op", "C");
        params.put("extrDatstId", datasetId);
        params.put("extrDmndId", dmndId);
        params.put("extrSpcfId", null);
        params.put("datstNm", datasetId);
        params.put("extrDatstSttsCd", dbStatus);
        params.put("sortNo", 1);
        params.put("mnfstCn", json);
        params.put("actorId", ACTOR);
        commandMapper.executeExtrDatst(params);
    }

    private void updateDatst(String datasetId, String dbStatus, String json) {
        Map<String, Object> params = new HashMap<>();
        params.put("op", "U");
        params.put("extrDatstId", datasetId);
        params.put("extrDatstSttsCd", dbStatus);
        params.put("mnfstCn", json);
        params.put("actorId", ACTOR);
        commandMapper.executeExtrDatst(params);
    }

    private void executeDatst(
            String op,
            String datasetId,
            String dmndId,
            String dbStatus,
            String json,
            Integer sortNo,
            String datstNm
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("op", op);
        params.put("extrDatstId", datasetId);
        params.put("extrDmndId", dmndId);
        params.put("extrDatstSttsCd", dbStatus);
        params.put("mnfstCn", json);
        params.put("sortNo", sortNo);
        params.put("datstNm", datstNm);
        params.put("actorId", ACTOR);
        commandMapper.executeExtrDatst(params);
    }

    private void executeDmnd(String op, String dmndId, String dbStatus) {
        Map<String, Object> params = new HashMap<>();
        params.put("op", op);
        params.put("extrDmndId", dmndId);
        params.put("extrDmndSttsCd", dbStatus);
        params.put("actorId", ACTOR);
        commandMapper.executeExtrDmnd(params);
    }

    private void insertExcn(
            String datasetId,
            String excnType,
            String excnStatus,
            Long rowCount,
            String storageType,
            String tableName,
            String filePath,
            String failMessage
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("op", "C");
        params.put("extrExcnId", null);
        params.put("extrDatstId", datasetId);
        params.put("excnTypeCd", excnType);
        params.put("excnSttsCd", excnStatus);
        params.put("rsltNocs", rowCount);
        params.put("rsltStrgTypeCd", storageType);
        params.put("rsltTblNm", tableName);
        params.put("rsltFilePath", filePath);
        params.put("failCn", failMessage);
        params.put("actorId", ACTOR);
        commandMapper.executeExtrExcn(params);
    }

    private static String toDbStatus(ExtractDatasetStatus status) {
        return switch (status) {
            case PREPARING -> "RUNNING";
            case PREPARED -> "PREPARED";
            case EXPORTING -> "EXPORTING";
            case COMPLETED, CLEANED -> "COMPLETED";
            case FAILED -> "FAILED";
        };
    }

    private static String resolveFileStorageType(ExtractDatasetManifest manifest) {
        if (manifest.exportFilePaths() == null || manifest.exportFilePaths().isEmpty()) {
            return "FILE";
        }
        boolean parquet = manifest.exportFilePaths().stream()
                .anyMatch(path -> path.toLowerCase().endsWith(".parquet"));
        return parquet ? "PARQUET" : "FILE";
    }

    private static String joinPaths(java.util.List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return null;
        }
        return paths.stream().collect(Collectors.joining(";"));
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }
}
