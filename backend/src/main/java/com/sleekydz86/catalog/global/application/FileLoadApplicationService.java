package com.sleekydz86.catalog.global.application;

import com.sleekydz86.catalog.domain.fileload.model.ConnectionProbeResult;
import com.sleekydz86.catalog.domain.fileload.model.FileColumnDef;
import com.sleekydz86.catalog.domain.fileload.model.FileLoadResult;
import com.sleekydz86.catalog.domain.fileload.model.FileTableSummary;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetDbExport;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetFormat;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetTemplate;
import com.sleekydz86.catalog.domain.fileload.service.FileLoadCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FileLoadApplicationService {

    private final FileLoadCommandService fileLoadCommandService;

    public FileLoadApplicationService(FileLoadCommandService fileLoadCommandService) {
        this.fileLoadCommandService = fileLoadCommandService;
    }

    public ConnectionProbeResult probe(String connectionId) {
        return fileLoadCommandService.probe(connectionId);
    }

    public List<FileTableSummary> listTables(String connectionId, String schemaName) {
        return fileLoadCommandService.listTables(connectionId, schemaName);
    }

    public List<FileColumnDef> listColumns(String connectionId, String schemaName, String tableName) {
        return fileLoadCommandService.listColumns(connectionId, schemaName, tableName);
    }

    @Transactional
    public void createTable(
            String connectionId,
            String schemaName,
            String tableName,
            String tableComment,
            List<FileColumnDef> columns
    ) {
        fileLoadCommandService.createTable(connectionId, schemaName, tableName, tableComment, columns);
    }

    public SpreadsheetTemplate template(
            String connectionId,
            String schemaName,
            String tableName,
            SpreadsheetFormat format
    ) {
        return fileLoadCommandService.downloadTemplate(connectionId, schemaName, tableName, format);
    }

    public SpreadsheetTemplate templateForColumns(
            String tableName,
            List<FileColumnDef> columns,
            SpreadsheetFormat format
    ) {
        return fileLoadCommandService.downloadTemplateForColumns(tableName, columns, format);
    }

    @Transactional
    public FileLoadResult upload(
            String connectionId,
            String schemaName,
            String tableName,
            SpreadsheetFormat format,
            InputStream inputStream
    ) {
        return fileLoadCommandService.upload(connectionId, schemaName, tableName, format, inputStream);
    }

    public SpreadsheetDbExport exportTables(
            String connectionId,
            String schemaName,
            List<String> tableNames,
            boolean allTables,
            SpreadsheetFormat format,
            String actorId
    ) {
        return fileLoadCommandService.exportTables(
                connectionId,
                schemaName,
                tableNames,
                allTables,
                format,
                actorId
        );
    }
}
