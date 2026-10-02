package com.sleekydz86.catalog.domain.fileload.service;

import com.sleekydz86.catalog.domain.connection.model.ConnectionHealthStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionPersistencePort;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionTestPort;
import com.sleekydz86.catalog.domain.connection.port.out.SecretCipherPort;
import com.sleekydz86.catalog.domain.fileload.model.ConnectionProbeResult;
import com.sleekydz86.catalog.domain.fileload.model.FileColumnDef;
import com.sleekydz86.catalog.domain.fileload.model.FileLoadResult;
import com.sleekydz86.catalog.domain.fileload.model.FileTableSummary;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetFormat;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetTemplate;
import com.sleekydz86.catalog.domain.fileload.port.out.FileLoadTargetPort;
import com.sleekydz86.catalog.domain.fileload.port.out.SpreadsheetDocumentPort;
import com.sleekydz86.catalog.domain.migration.model.ColumnSchema;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.migration.model.SourceTableDescriptor;
import com.sleekydz86.catalog.domain.migration.model.TableSchema;
import com.sleekydz86.catalog.domain.migration.port.out.SourceMetadataPort;
import com.sleekydz86.catalog.global.exception.BusinessException;
import com.sleekydz86.catalog.global.exception.ErrorCode;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FileLoadCommandService {

    private final ConnectionPersistencePort connectionPersistencePort;
    private final ConnectionTestPort connectionTestPort;
    private final SecretCipherPort secretCipherPort;
    private final SourceMetadataPort sourceMetadataPort;
    private final FileLoadTargetPort fileLoadTargetPort;
    private final SpreadsheetDocumentPort spreadsheetDocumentPort;

    public FileLoadCommandService(
            ConnectionPersistencePort connectionPersistencePort,
            ConnectionTestPort connectionTestPort,
            SecretCipherPort secretCipherPort,
            SourceMetadataPort sourceMetadataPort,
            FileLoadTargetPort fileLoadTargetPort,
            SpreadsheetDocumentPort spreadsheetDocumentPort
    ) {
        this.connectionPersistencePort = connectionPersistencePort;
        this.connectionTestPort = connectionTestPort;
        this.secretCipherPort = secretCipherPort;
        this.sourceMetadataPort = sourceMetadataPort;
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
