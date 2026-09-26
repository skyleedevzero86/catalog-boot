package com.sleekydz86.catalog.domain.migration.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "단일 테이블 동기 적재 결과")
public record LoadTableResult(
        @Schema(description = "적재한 테이블명", example = "EMPLOYEES")
        String tableName,
        @Schema(description = "타깃 스키마", example = "cdw")
        String targetSchema,
        @Schema(description = "실행된 CREATE TABLE DDL")
        String createTableDdl,
        @Schema(description = "적재된 행 수", example = "12500")
        long rowsLoaded,
        @Schema(description = "배치 실행 횟수", example = "25")
        int batchCount
) {
}
