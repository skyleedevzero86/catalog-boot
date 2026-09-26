package com.sleekydz86.catalog.domain.migration.model;

public record ColumnSchema(
        String name,
        String sourceTypeName,
        int jdbcType,
        Integer columnSize,
        Integer decimalDigits,
        boolean nullable,
        boolean primaryKey,
        int ordinalPosition
) {
}
