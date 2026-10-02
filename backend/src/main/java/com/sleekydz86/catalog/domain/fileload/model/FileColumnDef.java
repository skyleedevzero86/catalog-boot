package com.sleekydz86.catalog.domain.fileload.model;

public record FileColumnDef(
        String name,
        String sqlType,
        boolean nullable,
        String comment
) {
    public FileColumnDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("컬럼명은 비울 수 없습니다.");
        }
        if (sqlType == null || sqlType.isBlank()) {
            throw new IllegalArgumentException("컬럼 타입은 비울 수 없습니다. column=" + name);
        }
        name = name.trim();
        sqlType = sqlType.trim();
        comment = comment == null ? "" : comment.trim();
    }
}
