package com.sleekydz86.catalog.domain.migration.service;

import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import com.sleekydz86.catalog.domain.migration.model.ColumnSchema;
import com.sleekydz86.catalog.domain.migration.model.TableSchema;
import com.sleekydz86.catalog.domain.migration.port.out.DdlTypeMapperPort;
import com.sleekydz86.catalog.domain.migration.port.out.TargetDdlGeneratorPort;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TargetDdlGenerator implements TargetDdlGeneratorPort {

    private final DdlTypeMapperPort ddlTypeMapperPort;

    public TargetDdlGenerator(DdlTypeMapperPort ddlTypeMapperPort) {
        this.ddlTypeMapperPort = ddlTypeMapperPort;
    }

    @Override
    public String generateCreateTableDdl(DatabaseVendor targetVendor, String targetSchema, TableSchema tableSchema) {
        String qualified = qualify(targetVendor, targetSchema, tableSchema.tableName());
        List<String> columnDefs = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();

        for (ColumnSchema column : tableSchema.columns()) {
            StringBuilder def = new StringBuilder();
            def.append(quote(targetVendor, column.name()))
                    .append(' ')
                    .append(ddlTypeMapperPort.mapType(targetVendor, column));
            if (!column.nullable() && targetVendor != DatabaseVendor.CLICKHOUSE) {
                def.append(" NOT NULL");
            }
            columnDefs.add(def.toString());
            if (column.primaryKey()) {
                primaryKeys.add(quote(targetVendor, column.name()));
            }
        }

        if (!primaryKeys.isEmpty() && targetVendor != DatabaseVendor.CLICKHOUSE) {
            columnDefs.add("PRIMARY KEY (" + String.join(", ", primaryKeys) + ")");
        }

        String body = columnDefs.stream().collect(Collectors.joining(",\n  "));
        return switch (targetVendor) {
            case CLICKHOUSE -> {
                String orderBy = primaryKeys.isEmpty()
                        ? quote(targetVendor, tableSchema.columns().get(0).name())
                        : String.join(", ", primaryKeys);
                yield "CREATE TABLE " + qualified + " (\n  " + body + "\n) ENGINE = MergeTree() ORDER BY (" + orderBy + ")";
            }
            default -> "CREATE TABLE " + qualified + " (\n  " + body + "\n)";
        };
    }

    private String qualify(DatabaseVendor vendor, String schema, String table) {
        if (schema == null || schema.isBlank()) {
            return quote(vendor, table);
        }
        return quote(vendor, schema) + "." + quote(vendor, table);
    }

    private String quote(DatabaseVendor vendor, String identifier) {
        return switch (vendor) {
            case POSTGRESQL, ORACLE -> "\"" + identifier.replace("\"", "\"\"") + "\"";
            case MYSQL, MARIADB, CLICKHOUSE -> "`" + identifier.replace("`", "``") + "`";
        };
    }
}
