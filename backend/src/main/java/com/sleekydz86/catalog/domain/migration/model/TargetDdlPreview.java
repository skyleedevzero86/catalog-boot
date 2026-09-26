package com.sleekydz86.catalog.domain.migration.model;

import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "타깃 DB CREATE TABLE DDL 미리보기 결과")
public record TargetDdlPreview(
        @Schema(example = "EMPLOYEES")
        String tableName,
        DatabaseVendor sourceVendor,
        DatabaseVendor targetVendor,
        @Schema(description = "변환된 CREATE TABLE 문")
        String createTableDdl,
        @Schema(description = "컬럼 수", example = "12")
        int columnCount
) {
}