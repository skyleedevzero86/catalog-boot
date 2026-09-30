package com.sleekydz86.catalog.adapter.outbound.jdbc;


import com.sleekydz86.catalog.domain.migration.model.ColumnSchema;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.migration.model.SourceTableDescriptor;
import com.sleekydz86.catalog.domain.migration.model.TableSchema;
import com.sleekydz86.catalog.domain.migration.port.out.SourceMetadataPort;
import org.springframework.stereotype.Component;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class JdbcSourceMetadataAdapter implements SourceMetadataPort {

    private final JdbcConnectionProvider jdbcConnectionProvider;

    public JdbcSourceMetadataAdapter(JdbcConnectionProvider jdbcConnectionProvider) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
    }

    @Override
    public TableSchema readTable(DatabaseEndpoint source, String schemaName, String tableName) {
        return jdbcConnectionProvider.executeWithRetry(source, connection -> {
            try {
                return readTableInternal(connection, source, schemaName, tableName);
            } catch (SQLException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new SQLException(exception);
            }
        });
    }

    @Override
    public List<SourceTableDescriptor> listTables(DatabaseEndpoint source, String schemaName) {
        return jdbcConnectionProvider.executeWithRetry(source, connection -> {
            try {
                return listTablesInternal(connection, source, schemaName);
            } catch (SQLException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new SQLException(exception);
            }
        });
    }

    private TableSchema readTableInternal(Connection connection, DatabaseEndpoint source, String schemaName, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = schemaName == null || schemaName.isBlank() ? source.schemaName() : schemaName;

        Set<String> primaryKeys = new HashSet<>();
        try (ResultSet pk = metadata.getPrimaryKeys(catalog, schema, tableName)) {
            while (pk.next()) {
                primaryKeys.add(pk.getString("COLUMN_NAME"));
            }
        }

        List<ColumnSchema> columns = new ArrayList<>();
        try (ResultSet rs = metadata.getColumns(catalog, schema, tableName, null)) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                columns.add(new ColumnSchema(
                        name,
                        rs.getString("TYPE_NAME"),
                        rs.getInt("DATA_TYPE"),
                        rs.getObject("COLUMN_SIZE") == null ? null : rs.getInt("COLUMN_SIZE"),
                        rs.getObject("DECIMAL_DIGITS") == null ? null : rs.getInt("DECIMAL_DIGITS"),
                        "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")),
                        primaryKeys.contains(name),
                        rs.getInt("ORDINAL_POSITION")
                ));
            }
        }

        if (columns.isEmpty()) {
            throw new IllegalArgumentException("테이블을 찾을 수 없거나 컬럼이 없습니다: " + qualified(schema, tableName));
        }
        columns.sort(Comparator.comparingInt(ColumnSchema::ordinalPosition));
        return new TableSchema(schema, tableName, List.copyOf(columns));
    }

    private List<SourceTableDescriptor> listTablesInternal(Connection connection, DatabaseEndpoint source, String schemaName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = schemaName == null || schemaName.isBlank() ? source.schemaName() : schemaName;
        List<SourceTableDescriptor> tables = new ArrayList<>();
        try (ResultSet rs = metadata.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                if (name == null || name.isBlank()) {
                    continue;
                }
                tables.add(new SourceTableDescriptor(name, rs.getString("REMARKS")));
            }
        }
        tables.sort(Comparator.comparing(SourceTableDescriptor::name, String.CASE_INSENSITIVE_ORDER));
        return tables;
    }

    private String qualified(String schema, String table) {
        if (schema == null || schema.isBlank()) {
            return table;
        }
        return schema.toLowerCase(Locale.ROOT) + "." + table;
    }
}

