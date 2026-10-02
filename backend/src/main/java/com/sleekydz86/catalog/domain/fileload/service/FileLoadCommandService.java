package com.sleekydz86.catalog.domain.fileload.service;

import com.sleekydz86.catalog.domain.connection.model.ConnectionHealthStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionPersistencePort;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionTestPort;
import com.sleekydz86.catalog.domain.connection.port.out.SecretCipherPort;
import com.sleekydz86.catalog.domain.fileload.model.ConnectionProbeResult;
import com.sleekydz86.catalog.domain.fileload.model.ExportedTableData;
import com.sleekydz86.catalog.domain.fileload.model.FileColumnDef;
import com.sleekydz86.catalog.domain.fileload.model.FileLoadResult;
import com.sleekydz86.catalog.domain.fileload.model.FileTableSummary;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetDbExport;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetFormat;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetTemplate;
import com.sleekydz86.catalog.domain.fileload.port.out.FileLoadTargetPort;
import com.sleekydz86.catalog.domain.fileload.port.out.SpreadsheetDocumentPort;
import com.sleekydz86.catalog.domain.migration.model.ColumnSchema;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.migration.model.SourceTableDescriptor;
import com.sleekydz86.catalog.domain.migration.model.TableSchema;
import com.sleekydz86.catalog.domain.migration.port.out.SourceDataReaderPort;
import com.sleekydz86.catalog.domain.migration.port.out.SourceMetadataPort;
import com.sleekydz86.catalog.domain.migration.port.out.SourceTableBatchReader;
import com.sleekydz86.catalog.global.exception.BusinessException;
import com.sleekydz86.catalog.global.exception.ErrorCode;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FileLoadCommandService {

    private static final int EXPORT_BATCH_SIZE = 1_000;
    private static final int EXPORT_MAX_ROWS_PER_TABLE = 100_000;

    private final ConnectionPersistencePort connectionPersistencePort;
    private final ConnectionTestPort connectionTestPort;
    private final SecretCipherPort secretCipherPort;
    private final SourceMetadataPort sourceMetadataPort;
    private final SourceDataReaderPort sourceDataReaderPort;
    private final FileLoadTargetPort fileLoadTargetPort;
    private final SpreadsheetDocumentPort spreadsheetDocumentPort;

    public FileLoadCommandService(
            ConnectionPersistencePort connectionPersistencePort,
            ConnectionTestPort connectionTestPort,
            SecretCipherPort secretCipherPort,
            SourceMetadataPort sourceMetadataPort,
            SourceDataReaderPort sourceDataReaderPort,
            FileLoadTargetPort fileLoadTargetPort,
            SpreadsheetDocumentPort spreadsheetDocumentPort
    ) {
        this.connectionPersistencePort = connectionPersistencePort;
        this.connectionTestPort = connectionTestPort;
        this.secretCipherPort = secretCipherPort;
        this.sourceMetadataPort = sourceMetadataPort;
        this.sourceDataReaderPort = sourceDataReaderPort;
        this.fileLoadTargetPort = fileLoadTargetPort;
        this.spreadsheetDocumentPort = spreadsheetDocumentPort;
    }

    public ConnectionProbeResult probe(String connectionId) {
        ConnectionProfile profile = requireProfile(connectionId);
        String password = secretCipherPort.decrypt(profile.encryptedPassword());
        ConnectionHealthStatus status = connectionTestPort.test(profile, password);
        if (status == ConnectionHealthStatus.HEALTHY) {
            return new ConnectionProbeResult(
                    connectionId,
                    true,
                    "DB 접속에 성공했습니다."
            );
        }
        return new ConnectionProbeResult(
                connectionId,
                false,
                "DB 접속에 실패했습니다. 연결 프로필(호스트·포트·계정·비밀번호·방화벽)을 확인하세요."
        );
    }

    public List<FileTableSummary> listTables(String connectionId, String schemaName) {
        DatabaseEndpoint endpoint = toEndpoint(requireProfile(connectionId));
        requireConnected(connectionId);
        String schema = resolveSchema(endpoint, schemaName);
        return sourceMetadataPort.listTables(endpoint, schema).stream()
                .map(this::toSummary)
                .toList();
    }

    public List<FileColumnDef> listColumns(String connectionId, String schemaName, String tableName) {
        DatabaseEndpoint endpoint = toEndpoint(requireProfile(connectionId));
        requireConnected(connectionId);
        String schema = resolveSchema(endpoint, schemaName);
        TableSchema table = sourceMetadataPort.readTable(endpoint, schema, tableName.trim());
        return table.columns().stream().map(this::toColumnDef).toList();
    }

    public void createTable(
            String connectionId,
            String schemaName,
            String tableName,
            String tableComment,
            List<FileColumnDef> columns
    ) {
        if (columns == null || columns.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "생성할 컬럼이 없습니다.");
        }
        DatabaseEndpoint endpoint = toEndpoint(requireProfile(connectionId));
        requireConnected(connectionId);
        String schema = resolveSchema(endpoint, schemaName);
        fileLoadTargetPort.createTable(endpoint, schema, tableName.trim(), tableComment, columns);
    }

    public SpreadsheetTemplate downloadTemplate(
            String connectionId,
            String schemaName,
            String tableName,
            SpreadsheetFormat format
    ) {
        List<FileColumnDef> columns = listColumns(connectionId, schemaName, tableName);
        List<String> names = columns.stream().map(FileColumnDef::name).toList();
        return spreadsheetDocumentPort.buildTemplate(tableName.trim(), names, format);
    }

    public SpreadsheetTemplate downloadTemplateForColumns(
            String tableName,
            List<FileColumnDef> columns,
            SpreadsheetFormat format
    ) {
        if (columns == null || columns.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "템플릿 컬럼이 없습니다.");
        }
        List<String> names = columns.stream().map(FileColumnDef::name).toList();
        return spreadsheetDocumentPort.buildTemplate(tableName.trim(), names, format);
    }

    public FileLoadResult upload(
            String connectionId,
            String schemaName,
            String tableName,
            SpreadsheetFormat format,
            InputStream inputStream
    ) {
        List<FileColumnDef> columns = listColumns(connectionId, schemaName, tableName);
        List<String> columnNames = columns.stream().map(FileColumnDef::name).toList();
        List<List<String>> rawRows = spreadsheetDocumentPort.readDataRows(inputStream, format, columnNames);
        if (rawRows.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "업로드 파일에 데이터 행이 없습니다.");
        }
        DatabaseEndpoint endpoint = toEndpoint(requireProfile(connectionId));
        String schema = resolveSchema(endpoint, schemaName);
        List<List<Object>> rows = new ArrayList<>(rawRows.size());
        for (List<String> raw : rawRows) {
            rows.add(new ArrayList<>(raw));
        }
        long inserted = fileLoadTargetPort.insertRows(endpoint, schema, tableName.trim(), columnNames, rows);
        return new FileLoadResult(
                connectionId,
                schema,
                tableName.trim(),
                inserted,
                "파일 적재가 완료되었습니다. 적재 행 수=" + inserted
        );
    }

    public SpreadsheetDbExport exportTables(
            String connectionId,
            String schemaName,
            List<String> tableNames,
            boolean allTables,
            SpreadsheetFormat format,
            String actorId
    ) {
        ConnectionProfile profile = requireProfile(connectionId);
        requireConnected(connectionId);
        DatabaseEndpoint endpoint = toEndpoint(profile);
        String schema = resolveSchema(endpoint, schemaName);
        List<String> targets = resolveExportTables(endpoint, schema, tableNames, allTables);
        if (targets.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "추출할 테이블을 선택하세요.");
        }
        if (format == SpreadsheetFormat.CSV && targets.size() > 1) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "CSV는 테이블 1개만 지원합니다. 여러 테이블은 xlsx 또는 xls를 선택하세요."
            );
        }
        Set<String> usedSheetNames = new HashSet<>();
        usedSheetNames.add("목차");
        List<ExportedTableData> exported = new ArrayList<>();
        for (String table : targets) {
            TableSchema tableSchema = sourceMetadataPort.readTable(endpoint, schema, table);
            List<String> columns = tableSchema.columns().stream().map(ColumnSchema::name).toList();
            if (columns.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "컬럼이 없는 테이블은 추출할 수 없습니다. table=" + table
                );
            }
            List<List<String>> rows = readTableRows(endpoint, schema, table, columns);
            String sheetName = uniqueSheetName(table, usedSheetNames);
            exported.add(new ExportedTableData(schema, table, sheetName, columns, rows));
        }
        Instant extractedAt = Instant.now();
        String extractedBy = actorId == null || actorId.isBlank() ? "unknown" : actorId.trim();
        return spreadsheetDocumentPort.buildDbExport(format, connectionId, extractedBy, extractedAt, exported);
    }

    private List<String> resolveExportTables(
            DatabaseEndpoint endpoint,
            String schema,
            List<String> tableNames,
            boolean allTables
    ) {
        if (allTables) {
            return sourceMetadataPort.listTables(endpoint, schema).stream()
                    .map(SourceTableDescriptor::name)
                    .toList();
        }
        if (tableNames == null || tableNames.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String name : tableNames) {
            if (name != null && !name.isBlank()) {
                unique.add(name.trim());
            }
        }
        return List.copyOf(unique);
    }

    private List<List<String>> readTableRows(
            DatabaseEndpoint endpoint,
            String schema,
            String tableName,
            List<String> columns
    ) {
        List<List<String>> rows = new ArrayList<>();
        try (SourceTableBatchReader reader = sourceDataReaderPort.openTableReader(
                endpoint,
                schema,
                tableName,
                columns,
                EXPORT_BATCH_SIZE
        )) {
            while (rows.size() < EXPORT_MAX_ROWS_PER_TABLE) {
                List<Map<String, Object>> batch = reader.readNextBatch();
                if (batch == null || batch.isEmpty()) {
                    break;
                }
                for (Map<String, Object> map : batch) {
                    List<String> row = new ArrayList<>(columns.size());
                    for (String column : columns) {
                        Object value = map.get(column);
                        row.add(value == null ? "" : String.valueOf(value));
                    }
                    rows.add(row);
                    if (rows.size() >= EXPORT_MAX_ROWS_PER_TABLE) {
                        break;
                    }
                }
            }
        } catch (Exception exception) {
            throw new BusinessException(
                    ErrorCode.FILE_EXPORT_FAILED,
                    "테이블 데이터 읽기에 실패했습니다. table=" + tableName,
                    exception
            );
        }
        return rows;
    }

    private String uniqueSheetName(String tableName, Set<String> used) {
        String base = sanitizeSheetName(tableName);
        String candidate = base;
        int suffix = 2;
        while (used.contains(candidate.toLowerCase(Locale.ROOT)) || used.contains(candidate)) {
            String suffixText = "_" + suffix++;
            int room = Math.max(1, 31 - suffixText.length());
            candidate = (base.length() > room ? base.substring(0, room) : base) + suffixText;
        }
        used.add(candidate);
        used.add(candidate.toLowerCase(Locale.ROOT));
        return candidate;
    }

    private String sanitizeSheetName(String tableName) {
        String cleaned = tableName.replaceAll("[\\\\/*?:\\[\\]]", "_").trim();
        if (cleaned.isBlank()) {
            cleaned = "sheet";
        }
        return cleaned.length() <= 31 ? cleaned : cleaned.substring(0, 31);
    }

    private void requireConnected(String connectionId) {
        ConnectionProbeResult probe = probe(connectionId);
        if (!probe.connected()) {
            throw new BusinessException(ErrorCode.CONNECTION_UNHEALTHY, probe.message());
        }
    }

    private ConnectionProfile requireProfile(String connectionId) {
        if (connectionId == null || connectionId.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "connectionId는 필수입니다.");
        }
        return connectionPersistencePort.findById(connectionId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("등록된 연결을 찾을 수 없습니다: " + connectionId));
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

    private String resolveSchema(DatabaseEndpoint endpoint, String schemaName) {
        if (schemaName != null && !schemaName.isBlank()) {
            return schemaName.trim();
        }
        if (endpoint.schemaName() != null && !endpoint.schemaName().isBlank()) {
            return endpoint.schemaName().trim();
        }
        return endpoint.database();
    }

    private FileTableSummary toSummary(SourceTableDescriptor descriptor) {
        return new FileTableSummary(descriptor.name(), descriptor.remarks());
    }

    private FileColumnDef toColumnDef(ColumnSchema column) {
        String type = column.sourceTypeName() == null || column.sourceTypeName().isBlank()
                ? "VARCHAR"
                : column.sourceTypeName();
        return new FileColumnDef(column.name(), type, column.nullable(), "");
    }
}
