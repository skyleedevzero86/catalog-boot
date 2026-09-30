package com.sleekydz86.catalog.domain.extract.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Extract 작업 상태")
public enum ExtractJobStatus {
    SUBMITTED,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED,
    DISABLED
}

