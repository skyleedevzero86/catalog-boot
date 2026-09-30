package com.sleekydz86.catalog.domain.migration.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이그레이션 테이블 작업 상태")
public enum MigrationTableStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
}
