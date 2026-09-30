package com.sleekydz86.catalog.domain.extract.service;

import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;

public final class ExtractSqlBuilder {

    private ExtractSqlBuilder() {
    }

    public static String quoteIdentifier(DatabaseVendor vendor, String identifier) {
        return switch (vendor) {
            case POSTGRESQL, ORACLE -> "\"" + identifier.replace("\"", "\"\"") + "\"";
            case MYSQL, MARIADB, CLICKHOUSE -> "`" + identifier.replace("`", "``") + "`";
        };
    }

    public static String qualifiedTable(DatabaseVendor vendor, String schemaName, String tableName) {
        String schema = schemaName == null || schemaName.isBlank() ? null : schemaName;
        if (schema == null) {
            return quoteIdentifier(vendor, tableName);
        }
        return quoteIdentifier(vendor, schema) + "." + quoteIdentifier(vendor, tableName);
    }
}
