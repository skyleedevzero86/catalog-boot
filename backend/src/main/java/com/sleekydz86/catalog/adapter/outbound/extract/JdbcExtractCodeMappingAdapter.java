package com.sleekydz86.catalog.adapter.outbound.extract;

import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcConnectionProvider;
import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcSqlDialect;
import com.sleekydz86.catalog.adapter.outbound.staging.StagingMybatisGateway;
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
    private final StagingMybatisGateway stagingMybatisGateway;

    public JdbcExtractCodeMappingAdapter(
            JdbcConnectionProvider jdbcConnectionProvider,
            StagingMybatisGateway stagingMybatisGateway
    ) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
        this.stagingMybatisGateway = stagingMybatisGateway;
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
        stagingMybatisGateway.requirePostgreSQL(staging);
        String valueType = JdbcSqlDialect.stagingValueType(DatabaseVendor.POSTGRESQL);
        String columnsDdl = "\"code\" " + valueType + ", \"code_name\" " + valueType;
        stagingMybatisGateway.createTable(staging, schemaName, tableName, columnsDdl);
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
        List<List<Object>> payloadRows = new ArrayList<>(rows.size());
        for (CodeMappingRow row : rows) {
            List<Object> payloadRow = new ArrayList<>(2);
            payloadRow.add(row.code());
            payloadRow.add(row.codeName());
            payloadRows.add(payloadRow);
        }
        stagingMybatisGateway.insertRows(
                staging, schemaName, mappingTable, List.of("code", "code_name"), payloadRows
        );
    }

    private void dropTableIfExists(DatabaseEndpoint staging, String schemaName, String tableName) {
        stagingMybatisGateway.dropTable(staging, schemaName, tableName);
    }

    private record CodeMappingRow(String code, String codeName) {
    }
}
