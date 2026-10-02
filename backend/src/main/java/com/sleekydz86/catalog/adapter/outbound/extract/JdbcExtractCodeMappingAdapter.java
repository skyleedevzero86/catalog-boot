package com.sleekydz86.catalog.adapter.outbound.extract;

import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcConnectionProvider;
import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcSqlDialect;
import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import com.sleekydz86.catalog.domain.extract.model.ExtractCodeMappingSpec;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractCodeMappingPort;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import org.springframework.stereotype.Component;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class JdbcExtractCodeMappingAdapter implements ExtractCodeMappingPort {

    private final JdbcConnectionProvider jdbcConnectionProvider;

    public JdbcExtractCodeMappingAdapter(JdbcConnectionProvider jdbcConnectionProvider) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
    }

    @Override
    public String createAndPopulateMappingTable(
            DatabaseEndpoint source,
            DatabaseEndpoint staging,
            String stagingSchema,
            String stagingTableName,
            String datasetId,
            ExtractCodeMappingSpec mapping,
            String physicalColumnName
    ) {
        String mappingTable = JdbcSqlDialect.mappingTableName(datasetId, mapping.sourceColumnKey());
        dropTableIfExists(staging, stagingSchema, mappingTable);
        createMappingTable(staging, stagingSchema, mappingTable);
        Set<String> codes = distinctCodes(staging, stagingSchema, stagingTableName, physicalColumnName);
        if (codes.isEmpty()) {
            return mappingTable;
        }
        populateMappingTable(source, staging, stagingSchema, mappingTable, mapping, codes);
        return mappingTable;
    }

    private void createMappingTable(DatabaseEndpoint staging, String schemaName, String tableName) {
        String qualified = JdbcSqlDialect.qualifiedName(staging.vendor(), schemaName, staging.schemaName(), tableName);
        String valueType = JdbcSqlDialect.stagingValueType(staging.vendor());
        String ddl = switch (staging.vendor()) {
            case CLICKHOUSE -> "CREATE TABLE " + qualified + " ("
                    + JdbcSqlDialect.quoteIdentifier(staging.vendor(), "code") + " " + valueType + ", "
                    + JdbcSqlDialect.quoteIdentifier(staging.vendor(), "code_name") + " " + valueType
                    + ") ENGINE = MergeTree() ORDER BY code";
            default -> "CREATE TABLE " + qualified + " ("
                    + JdbcSqlDialect.quoteIdentifier(staging.vendor(), "code") + " " + valueType + ", "
                    + JdbcSqlDialect.quoteIdentifier(staging.vendor(), "code_name") + " " + valueType + ")";
        };
        jdbcConnectionProvider.runWithRetry(staging, connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(ddl);
            }
        });
    }

    private Set<String> distinctCodes(
            DatabaseEndpoint staging,
            String schemaName,
            String stagingTableName,
            String physicalColumnName
    ) {
        String qualified = JdbcSqlDialect.qualifiedName(staging.vendor(), schemaName, staging.schemaName(), stagingTableName);
        String column = JdbcSqlDialect.quoteIdentifier(staging.vendor(), physicalColumnName);
        String sql = "SELECT DISTINCT " + column + " FROM " + qualified + " WHERE " + column + " IS NOT NULL";
        return jdbcConnectionProvider.executeWithRetry(staging, connection -> {
            Set<String> codes = new HashSet<>();
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {
                while (resultSet.next()) {
                    String code = resultSet.getString(1);
                    if (code != null && !code.isBlank()) {
                        codes.add(code);
                    }
                }
            }
            return codes;
        });
    }

    private void populateMappingTable(
            DatabaseEndpoint source,
            DatabaseEndpoint staging,
            String stagingSchema,
            String mappingTable,
            ExtractCodeMappingSpec mapping,
            Set<String> codes
    ) {
        List<String> codeList = new ArrayList<>(codes);
        int batchSize = 500;
        for (int offset = 0; offset < codeList.size(); offset += batchSize) {
            List<String> batch = codeList.subList(offset, Math.min(offset + batchSize, codeList.size()));
            List<CodeMappingRow> rows = lookupCodes(source, mapping, batch);
            insertMappings(staging, stagingSchema, mappingTable, rows);
        }
    }

    private List<CodeMappingRow> lookupCodes(
            DatabaseEndpoint source,
            ExtractCodeMappingSpec mapping,
            List<String> codes
    ) {
        if (codes.isEmpty()) {
            return List.of();
        }
        String schema = mapping.schemaName() == null || mapping.schemaName().isBlank()
                ? source.schemaName()
                : mapping.schemaName();
        String qualified = JdbcSqlDialect.qualifiedName(source.vendor(), schema, mapping.codeTableName());
        String codeColumn = JdbcSqlDialect.quoteIdentifier(source.vendor(), mapping.codeColumnName());
        String nameColumn = JdbcSqlDialect.quoteIdentifier(source.vendor(), mapping.codeNameColumnName());
        String placeholders = codes.stream().map(ignored -> "?").collect(java.util.stream.Collectors.joining(", "));
        String sql = "SELECT " + codeColumn + ", " + nameColumn + " FROM " + qualified
                + " WHERE " + codeColumn + " IN (" + placeholders + ")";

        return jdbcConnectionProvider.executeWithRetry(source, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(JdbcSqlDialect.driverSql(source.vendor(), sql))) {
                for (int i = 0; i < codes.size(); i++) {
                    statement.setString(i + 1, codes.get(i));
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<CodeMappingRow> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        rows.add(new CodeMappingRow(
                                resultSet.getString(1),
                                resultSet.getString(2)
                        ));
                    }
                    return rows;
                }
            }
        });
    }

    private void insertMappings(
            DatabaseEndpoint staging,
            String schemaName,
            String mappingTable,
            List<CodeMappingRow> rows
    ) {
        if (rows.isEmpty()) {
            return;
        }
        String qualified = JdbcSqlDialect.qualifiedName(staging.vendor(), schemaName, staging.schemaName(), mappingTable);
        String sql = "INSERT INTO " + qualified + " ("
                + JdbcSqlDialect.quoteIdentifier(staging.vendor(), "code") + ", "
                + JdbcSqlDialect.quoteIdentifier(staging.vendor(), "code_name")
                + ") VALUES (?, ?)";
        jdbcConnectionProvider.runWithRetry(staging, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (CodeMappingRow row : rows) {
                    statement.setString(1, row.code());
                    statement.setString(2, row.codeName());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    private void dropTableIfExists(DatabaseEndpoint staging, String schemaName, String tableName) {
        jdbcConnectionProvider.runWithRetry(staging, connection -> {
            String qualified = JdbcSqlDialect.qualifiedName(staging.vendor(), schemaName, staging.schemaName(), tableName);
            String sql = switch (staging.vendor()) {
                case POSTGRESQL -> "DROP TABLE IF EXISTS " + qualified + " CASCADE";
                case MYSQL, MARIADB, CLICKHOUSE -> "DROP TABLE IF EXISTS " + qualified;
                case ORACLE -> JdbcSqlDialect.oracleDropTablePlSql(qualified);
            };
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        });
    }

    private record CodeMappingRow(String code, String codeName) {
    }
}
