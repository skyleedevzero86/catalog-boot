package com.sleekydz86.catalog.domain.extract.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "추출 데이터셋 상태")
public enum ExtractDatasetStatus {
    PREPARING,
    PREPARED,
    EXPORTING,
    COMPLETED,
    FAILED,
    CLEANED
}
