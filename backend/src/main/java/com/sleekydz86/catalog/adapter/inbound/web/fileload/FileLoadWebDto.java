package com.sleekydz86.catalog.adapter.inbound.web.fileload;

import com.sleekydz86.catalog.domain.fileload.model.FileColumnDef;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class FileLoadWebDto {

    private FileLoadWebDto() {
    }

    @Schema(name = "FileLoadProbeResponse")
    public record ProbeResponse(
            String connectionId,
            boolean connected,
            String message
    ) {
    }

    @Schema(name = "FileTableSummaryResponse")
    public record TableSummaryResponse(
            String tableName,
            String remarks
    ) {
    }

    @Schema(name = "FileColumnDefRequest")
    public record ColumnRequest(
            @NotBlank String name,
            @NotBlank String sqlType,
            @NotNull Boolean nullable,
            String comment
    ) {
        public FileColumnDef toDomain() {
            return new FileColumnDef(name, sqlType, Boolean.TRUE.equals(nullable), comment);
        }
    }

    @Schema(name = "FileColumnDefResponse")
    public record ColumnResponse(
            String name,
            String sqlType,
            boolean nullable,
            String comment
    ) {
        public static ColumnResponse from(FileColumnDef column) {
            return new ColumnResponse(column.name(), column.sqlType(), column.nullable(), column.comment());
        }
    }

    @Schema(name = "CreateFileTableRequest")
    public record CreateTableRequest(
            @NotBlank String connectionId,
            String schemaName,
            @NotBlank String tableName,
            String tableComment,
            @NotEmpty @Valid List<ColumnRequest> columns
    ) {
    }

    @Schema(name = "FileTemplateColumnsRequest")
    public record TemplateColumnsRequest(
            @NotBlank String tableName,
            @NotEmpty @Valid List<ColumnRequest> columns,
            @NotBlank String format
    ) {
    }

    @Schema(name = "FileLoadResultResponse")
    public record LoadResultResponse(
            String connectionId,
            String schemaName,
            String tableName,
            long insertedRows,
            String message
    ) {
    }

    @Schema(name = "DbExportRequest")
    public record DbExportRequest(
            @NotBlank String connectionId,
            String schemaName,
            List<String> tableNames,
            Boolean allTables,
            @NotBlank String format
    ) {
    }
}
