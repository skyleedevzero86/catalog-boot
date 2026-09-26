package com.sleekydz86.catalog.domain.migration.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이그레이션 배치 작업 상태")
public enum MigrationJobStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    CANCELLED
}
