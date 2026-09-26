package com.sleekydz86.catalog.domain.metadata.service;

import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionPersistencePort;
import com.sleekydz86.catalog.domain.connection.port.out.SecretCipherPort;
import com.sleekydz86.catalog.domain.metadata.model.MetaSet;
import com.sleekydz86.catalog.domain.metadata.model.MetaSyncResult;
import com.sleekydz86.catalog.domain.metadata.model.MetaSyncStatus;
import com.sleekydz86.catalog.domain.metadata.model.MetaTable;
import com.sleekydz86.catalog.domain.metadata.model.SyncMetadataCommand;
import com.sleekydz86.catalog.domain.metadata.port.out.MetaPersistencePort;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.migration.model.SourceTableDescriptor;
import com.sleekydz86.catalog.domain.migration.port.out.SourceMetadataPort;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MetaSyncService {

    private static final int DISPLAY_NAME_MAX_LENGTH = 255;

    private final MetaPersistencePort metaPersistencePort;
    private final ConnectionPersistencePort connectionPersistencePort;
    private final SourceMetadataPort sourceMetadataPort;
    private final SecretCipherPort secretCipherPort;

    public MetaSyncService(
            MetaPersistencePort metaPersistencePort,
            ConnectionPersistencePort connectionPersistencePort,
            SourceMetadataPort sourceMetadataPort,
            SecretCipherPort secretCipherPort
    ) {
        this.metaPersistencePort = metaPersistencePort;
        this.connectionPersistencePort = connectionPersistencePort;
        this.sourceMetadataPort = sourceMetadataPort;
        this.secretCipherPort = secretCipherPort;
    }

    public MetaSyncResult handle(SyncMetadataCommand command) {
        MetaSet metaSet = metaPersistencePort.findMetaSetById(command.mtdtId())
                .orElseThrow(() -> new ResourceNotFoundException("메타데이터 세트를 찾을 수 없습니다: " + command.mtdtId()));
        ConnectionProfile connection = connectionPersistencePort.findById(metaSet.connectionId())
                .orElseThrow(() -> new ResourceNotFoundException("연결을 찾을 수 없습니다: " + metaSet.connectionId()));

        metaPersistencePort.saveMetaSet(metaSet.withSyncStatus(
                MetaSyncStatus.RUNNING,
                Instant.now(),
                null,
                command.actorId()
        ));

        try {
            SyncCounts counts = synchronizeTables(metaSet, connection, command.actorId());
            String message = "동기화 완료: 추가 %d, 누락 %d, 복원 %d".formatted(
                    counts.addedCount(), counts.missingCount(), counts.restoredCount()
            );
            MetaSet completed = metaPersistencePort.saveMetaSet(metaSet.withSyncStatus(
                    MetaSyncStatus.SUCCESS,
                    Instant.now(),
                    message,
                    command.actorId()
            ));
            return new MetaSyncResult(
                    completed.id(),
                    completed.lastSyncStatus(),
                    completed.lastSyncAt(),
                    completed.lastSyncMessage(),
                    counts.tableCount(),
                    counts.addedCount(),
                    counts.missingCount(),
                    counts.restoredCount()
            );
        } catch (Exception exception) {
            String message = truncate(exception.getMessage(), 1000);
            metaPersistencePort.saveMetaSet(metaSet.withSyncStatus(
                    MetaSyncStatus.FAILED,
                    Instant.now(),
                    message,
                    command.actorId()
            ));
            throw new IllegalStateException("메타데이터 동기화에 실패했습니다: " + message, exception);
        }
    }

    private SyncCounts synchronizeTables(MetaSet metaSet, ConnectionProfile connection, String actorId) {
        DatabaseEndpoint endpoint = toEndpoint(connection);
        String schemaName = resolveSchemaName(connection);
        List<SourceTableDescriptor> discoveredTables = sourceMetadataPort.listTables(endpoint, schemaName);
        if (discoveredTables.isEmpty()) {
            throw new IllegalStateException("스키마에서 테이블을 찾을 수 없습니다: " + schemaName);
        }

        List<MetaTable> existingTables = metaPersistencePort.findSourceTablesByMtdtId(metaSet.id());
        Map<String, MetaTable> existingByName = new LinkedHashMap<>();
        existingTables.forEach(table -> existingByName.put(tableKey(table.originalTableName()), table));

        Set<String> discoveredKeys = new HashSet<>();
        int nextSortNo = existingTables.stream().mapToInt(MetaTable::sortNo).max().orElse(0) + 1;
        int addedCount = 0;
        int missingCount = 0;
        int restoredCount = 0;

        for (SourceTableDescriptor discovered : discoveredTables) {
            String originalName = discovered.name();
            String key = tableKey(originalName);
            discoveredKeys.add(key);

            MetaTable table = existingByName.get(key);
            if (table == null) {
                table = MetaTable.createNew(
                        metaSet.id(),
                        originalName,
                        initialDisplayName(discovered.remarks(), originalName),
                        trimToNull(discovered.remarks()),
                        nextSortNo++,
                        actorId
                );
                addedCount++;
            } else {
                if (!table.sourceExists()) {
                    restoredCount++;
                }
                table = table.withSourceSync(
                        trimToNull(discovered.remarks()),
                        true,
                        true,
                        actorId
                );
            }
            metaPersistencePort.saveMetaTable(table);
        }

        for (MetaTable table : existingTables) {
            if (discoveredKeys.contains(tableKey(table.originalTableName()))) {
                continue;
            }
            if (table.sourceExists()) {
                missingCount++;
            }
            metaPersistencePort.saveMetaTable(table.withSourceSync(
                    table.originalTableDescription(),
                    false,
                    table.useYn(),
                    actorId
            ));
        }

        int tableCount = (int) metaPersistencePort.findSourceTablesByMtdtId(metaSet.id()).stream()
                .filter(table -> table.sourceExists() && table.useYn())
                .count();
        return new SyncCounts(tableCount, addedCount, missingCount, restoredCount);
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

    private String resolveSchemaName(ConnectionProfile profile) {
        if (profile.schemaName() != null && !profile.schemaName().isBlank()) {
            return profile.schemaName().trim();
        }
        return profile.databaseName();
    }

    private String tableKey(String tableName) {
        return tableName == null ? "" : tableName.toLowerCase(Locale.ROOT);
    }

    private String initialDisplayName(String remarks, String fallback) {
        String candidate = trimToNull(remarks);
        if (candidate == null) {
            candidate = fallback;
        }
        return candidate.length() <= DISPLAY_NAME_MAX_LENGTH
                ? candidate
                : candidate.substring(0, DISPLAY_NAME_MAX_LENGTH);
    }

    private String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record SyncCounts(
            int tableCount,
            int addedCount,
            int missingCount,
            int restoredCount
    ) {
    }
}
