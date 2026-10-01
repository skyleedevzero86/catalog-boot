package com.sleekydz86.catalog.domain.migration.service;

import com.sleekydz86.catalog.domain.migration.model.ColumnSchema;
import com.sleekydz86.catalog.domain.migration.model.LoadTableResult;
import com.sleekydz86.catalog.domain.migration.model.LoadTableToTargetCommand;
import com.sleekydz86.catalog.domain.migration.model.PreviewTargetDdlCommand;
import com.sleekydz86.catalog.domain.migration.model.TableSchema;
import com.sleekydz86.catalog.domain.migration.model.TargetDdlPreview;
import com.sleekydz86.catalog.domain.migration.port.out.SourceDataReaderPort;
import com.sleekydz86.catalog.domain.migration.port.out.SourceMetadataPort;
import com.sleekydz86.catalog.domain.migration.port.out.SourceTableBatchReader;
import com.sleekydz86.catalog.domain.migration.port.out.TargetDatabasePort;
import com.sleekydz86.catalog.domain.migration.port.out.TargetDdlGeneratorPort;
import com.sleekydz86.catalog.global.application.MigrationJobCancellationRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MigrationCommandService {

    private final SourceMetadataPort sourceMetadataPort;
    private final SourceDataReaderPort sourceDataReaderPort;
    private final TargetDatabasePort targetDatabasePort;
    private final TargetDdlGeneratorPort targetDdlGeneratorPort;

    public MigrationCommandService(
            SourceMetadataPort sourceMetadataPort,
            SourceDataReaderPort sourceDataReaderPort,
            TargetDatabasePort targetDatabasePort,
            TargetDdlGeneratorPort targetDdlGeneratorPort
    ) {
        this.sourceMetadataPort = sourceMetadataPort;
        this.sourceDataReaderPort = sourceDataReaderPort;
        this.targetDatabasePort = targetDatabasePort;
        this.targetDdlGeneratorPort = targetDdlGeneratorPort;
    }

    public TargetDdlPreview handle(PreviewTargetDdlCommand command) {
        TableSchema table = requireTable(command.source(), command.sourceSchema(), command.tableName());
        String ddl = targetDdlGeneratorPort.generateCreateTableDdl(
                command.targetVendor(),
                command.targetSchema(),
                table
        );
        return new TargetDdlPreview(
                table.tableName(),
                command.source().vendor(),
                command.targetVendor(),
                ddl,
                table.columns().size()
        );
    }

    public LoadTableResult handle(LoadTableToTargetCommand command) {
        TableSchema table = requireTable(command.source(), command.sourceSchema(), command.tableName());
        List<String> columnNames = table.columns().stream().map(ColumnSchema::name).toList();
        String ddl = targetDdlGeneratorPort.generateCreateTableDdl(
                command.target().vendor(),
                command.targetSchema(),
                table
        );

        if (command.dropExisting()) {
            targetDatabasePort.dropTableIfExists(command.target(), command.targetSchema(), command.tableName());
        }
        targetDatabasePort.executeDdl(command.target(), ddl);

        long rowsLoaded = 0L;
        int batchCount = 0;
        try (SourceTableBatchReader reader = sourceDataReaderPort.openTableReader(
                command.source(),
                command.sourceSchema(),
                command.tableName(),
                columnNames,
                command.batchSize()
        )) {
            while (true) {
                if (command.cancelJobId() != null && MigrationJobCancellationRegistry.isCancelled(command.cancelJobId())) {
                    throw new IllegalStateException("마이그레이션 작업이 취소되었습니다: " + command.cancelJobId());
                }
                List<Map<String, Object>> batch = reader.readNextBatch();
                if (batch.isEmpty()) {
                    break;
                }
                List<List<Object>> values = new ArrayList<>(batch.size());
                for (Map<String, Object> row : batch) {
                    List<Object> ordered = new ArrayList<>(columnNames.size());
                    for (String columnName : columnNames) {
                        ordered.add(row.get(columnName));
                    }
                    values.add(ordered);
                }
                rowsLoaded += targetDatabasePort.batchInsert(
                        command.target(),
                        command.targetSchema(),
                        command.tableName(),
                        columnNames,
                        values
                );
                batchCount++;
            }
        }

        return new LoadTableResult(
                command.tableName(),
                command.targetSchema(),
                ddl,
                rowsLoaded,
                batchCount
        );
    }

    private TableSchema requireTable(
            com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint source,
            String schemaName,
            String tableName
    ) {
        TableSchema table = sourceMetadataPort.readTable(source, schemaName, tableName);
        if (table == null || table.columns() == null || table.columns().isEmpty()) {
            throw new IllegalArgumentException("원천 테이블 메타데이터를 찾을 수 없습니다: " + tableName);
        }
        return table;
    }
}
