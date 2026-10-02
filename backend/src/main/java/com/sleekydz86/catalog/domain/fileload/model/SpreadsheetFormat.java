package com.sleekydz86.catalog.domain.fileload.model;

import com.sleekydz86.catalog.global.exception.BusinessException;
import com.sleekydz86.catalog.global.exception.ErrorCode;

public enum SpreadsheetFormat {
    CSV,
    XLSX,
    XLS;

    public static SpreadsheetFormat from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "파일 형식(format)은 필수입니다. 예: csv, xlsx, xls");
        }
        String normalized = raw.trim().toLowerCase();
        return switch (normalized) {
            case "csv" -> CSV;
            case "xlsx" -> XLSX;
            case "xls" -> XLS;
            default -> throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "지원하지 않는 파일 형식입니다: " + raw + " (허용: csv, xlsx, xls)"
            );
        };
    }

    public String fileExtension() {
        return name().toLowerCase();
    }

    public String contentType() {
        return switch (this) {
            case CSV -> "text/csv";
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case XLS -> "application/vnd.ms-excel";
        };
    }
}
