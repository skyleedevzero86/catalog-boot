package com.sleekydz86.catalog.domain.migration.service;

import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import com.sleekydz86.catalog.domain.migration.model.ColumnSchema;
import com.sleekydz86.catalog.domain.migration.port.out.DdlTypeMapperPort;

import java.sql.Types;
import java.util.Locale;

public class DdlTypeMapper implements DdlTypeMapperPort {

    @Override
    public String mapType(DatabaseVendor targetVendor, ColumnSchema column) {
        String source = column.sourceTypeName() == null ? "" : column.sourceTypeName().toUpperCase(Locale.ROOT);
        return switch (targetVendor) {
            case POSTGRESQL -> mapPostgreSql(source, column);
            case MYSQL, MARIADB -> mapMySql(source, column);
            case ORACLE -> mapOracle(source, column);
            case CLICKHOUSE -> mapClickHouse(source, column);
        };
    }

    private String mapPostgreSql(String source, ColumnSchema column) {
        if (isInteger(source, column.jdbcType())) {
            return column.columnSize() != null && column.columnSize() > 9 ? "BIGINT" : "INTEGER";
        }
        if (isDecimal(source, column.jdbcType())) {
            return "NUMERIC(" + size(column, 38) + "," + scale(column) + ")";
        }
        if (isBoolean(source, column.jdbcType())) {
            return "BOOLEAN";
        }
        if (isDate(source, column.jdbcType())) {
            return "DATE";
        }
        if (isTimestamp(source, column.jdbcType())) {
            return "TIMESTAMP";
        }
        if (isBinary(source, column.jdbcType())) {
            return "BYTEA";
        }
        if (isClob(source, column.jdbcType()) || size(column, 0) > 4000) {
            return "TEXT";
        }
        return "VARCHAR(" + Math.max(1, size(column, 255)) + ")";
    }

    private String mapMySql(String source, ColumnSchema column) {
        if (isInteger(source, column.jdbcType())) {
            return column.columnSize() != null && column.columnSize() > 9 ? "BIGINT" : "INT";
        }
        if (isDecimal(source, column.jdbcType())) {
            return "DECIMAL(" + size(column, 38) + "," + scale(column) + ")";
        }
        if (isBoolean(source, column.jdbcType())) {
            return "TINYINT(1)";
        }
        if (isDate(source, column.jdbcType())) {
            return "DATE";
        }
        if (isTimestamp(source, column.jdbcType())) {
            return "DATETIME(6)";
        }
        if (isBinary(source, column.jdbcType())) {
            return "LONGBLOB";
        }
        if (isClob(source, column.jdbcType()) || size(column, 0) > 4000) {
            return "LONGTEXT";
        }
        return "VARCHAR(" + Math.max(1, size(column, 255)) + ")";
    }

    private String mapOracle(String source, ColumnSchema column) {
        if (isInteger(source, column.jdbcType()) || isDecimal(source, column.jdbcType()) || source.contains("NUMBER")) {
            if (column.decimalDigits() == null || column.decimalDigits() == 0) {
                return "NUMBER(" + size(column, 38) + ")";
            }
            return "NUMBER(" + size(column, 38) + "," + scale(column) + ")";
        }
        if (isBoolean(source, column.jdbcType())) {
            return "NUMBER(1)";
        }
        if (isDate(source, column.jdbcType()) || isTimestamp(source, column.jdbcType())) {
            return "TIMESTAMP";
        }
        if (isBinary(source, column.jdbcType())) {
            return "BLOB";
        }
        if (isClob(source, column.jdbcType()) || size(column, 0) > 4000) {
            return "CLOB";
        }
        return "VARCHAR2(" + Math.max(1, Math.min(4000, size(column, 255))) + ")";
    }

    private String mapClickHouse(String source, ColumnSchema column) {
        String base;
        if (isInteger(source, column.jdbcType())) {
            base = column.columnSize() != null && column.columnSize() > 9 ? "Int64" : "Int32";
        } else if (isDecimal(source, column.jdbcType())) {
            base = "Decimal(" + size(column, 38) + ", " + scale(column) + ")";
        } else if (isBoolean(source, column.jdbcType())) {
            base = "UInt8";
        } else if (isDate(source, column.jdbcType())) {
            base = "Date";
        } else if (isTimestamp(source, column.jdbcType())) {
            base = "DateTime64(3)";
        } else if (isBinary(source, column.jdbcType())) {
            base = "String";
        } else {
            base = "String";
        }
        return column.nullable() ? "Nullable(" + base + ")" : base;
    }

    private boolean isInteger(String source, int jdbcType) {
        return source.contains("INT") || source.contains("SERIAL") || source.equals("NUMBER")
                || jdbcType == Types.INTEGER || jdbcType == Types.BIGINT || jdbcType == Types.SMALLINT
                || jdbcType == Types.TINYINT || jdbcType == Types.NUMERIC;
    }

    private boolean isDecimal(String source, int jdbcType) {
        return source.contains("DECIMAL") || source.contains("NUMERIC") || source.contains("NUMBER")
                || source.contains("FLOAT") || source.contains("DOUBLE") || source.contains("REAL")
                || jdbcType == Types.DECIMAL || jdbcType == Types.NUMERIC
                || jdbcType == Types.FLOAT || jdbcType == Types.DOUBLE || jdbcType == Types.REAL;
    }

    private boolean isBoolean(String source, int jdbcType) {
        return source.contains("BOOL") || source.contains("BIT") || jdbcType == Types.BOOLEAN || jdbcType == Types.BIT;
    }

    private boolean isDate(String source, int jdbcType) {
        return source.equals("DATE") || jdbcType == Types.DATE;
    }

    private boolean isTimestamp(String source, int jdbcType) {
        return source.contains("TIMESTAMP") || source.contains("DATETIME")
                || jdbcType == Types.TIMESTAMP || jdbcType == Types.TIMESTAMP_WITH_TIMEZONE;
    }

    private boolean isBinary(String source, int jdbcType) {
        return source.contains("BLOB") || source.contains("BINARY") || source.contains("BYTEA")
                || jdbcType == Types.BLOB || jdbcType == Types.BINARY || jdbcType == Types.VARBINARY
                || jdbcType == Types.LONGVARBINARY;
    }

    private boolean isClob(String source, int jdbcType) {
        return source.contains("CLOB") || source.contains("TEXT") || source.contains("JSON")
                || jdbcType == Types.CLOB || jdbcType == Types.LONGVARCHAR || jdbcType == Types.LONGNVARCHAR;
    }

    private int size(ColumnSchema column, int fallback) {
        return column.columnSize() == null || column.columnSize() <= 0 ? fallback : column.columnSize();
    }

    private int scale(ColumnSchema column) {
        return column.decimalDigits() == null || column.decimalDigits() < 0 ? 0 : column.decimalDigits();
    }
}
