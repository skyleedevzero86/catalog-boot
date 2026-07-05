package com.sleekydz86.catalog.domain.metadata.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메타데이터 동기화 상태")
public enum MetaSyncStatus {
    NEVER,
    RUNNING,
    SUCCESS,
    FAILED
}
