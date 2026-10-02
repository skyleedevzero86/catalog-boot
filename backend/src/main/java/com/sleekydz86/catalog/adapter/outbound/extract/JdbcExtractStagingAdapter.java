package com.sleekydz86.catalog.adapter.outbound.extract;

import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcConnectionProvider;
import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcSqlDialect;
import com.sleekydz86.catalog.adapter.outbound.staging.StagingMybatisGateway;
import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import com.sleekydz86.catalog.domain.extract.model.ValidatedExtractQuery;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractStagingPort;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.migration.port.out.SourceDataReaderPort;
import com.sleekydz86.catalog.domain.migration.port.out.SourceTableBatchReader;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JdbcExtractStagingAdapter implements ExtractStagingPort {

    private final JdbcConnectionProvider jdbcConnectionProvider;
    private final SourceDataReaderPort sourceDataReaderPort;
    private final StagingMybatisGateway stagingMybatisGateway;

    public JdbcExtractStagingAdapter(
            JdbcConnectionProvider jdbcConnectionProvider,
            SourceDataReaderPort sourceDataReaderPort,
            StagingMybatisGateway stagingMybatisGateway
    ) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
        this.sourceDataReaderPort = sourceDataReaderPort;
        this.stagingMybatisGateway = stagingMybatisGateway;
    }

    @Override
    public TableNames tableNames(String datasetId) {
        return new TableNames(
                JdbcSqlDialect.stagingRawTableName(datasetId),
                JdbcSqlDialect.stagingTableName(datasetId)
        );
    }

    @Override
    public String physicalColumnName(int index) {
        return JdbcSqlDialect.physicalColumnName(index);
    }

    @Override
    public void dropTableIfExists(DatabaseEndpoint staging, String schemaName, String tableName) {
        stagingMybatisGateway.dropTable(staging, schemaName, tableName);
    }

    @Override
    public void createRawStagingTable(
            DatabaseEndpoint staging,
            String schemaName,
            String tableName,
            List<String> physicalColumnNames
    ) {
        stagingMybatisGateway.requirePostgreSQL(staging);
        String valueType = JdbcSqlDialect.stagingValueType(DatabaseVendor.POSTGRESQL);
        StringBuilder body = new StringBuilder();
        body.append("\"__row_no\" BIGINT NOT NULL,\n");
        body.append("\"__row_hash\" VARCHAR(64),\n");
        for (String column : physicalColumnNames) {
            body.append(JdbcSqlDialect.quoteIdentifier(DatabaseVendor.POSTGRESQL, column))
                    .append(" ").append(valueType).append(",\n");
        }
        body.setLength(body.length() - 2);
        stagingMybatisGateway.createTable(staging, schemaName, tableName, body.toString());
    }

    @Override
    public long loadFromTable(
            DatabaseEndpoint source,
            String sourceSchema,
            String tableName,
            List<String> sourceColumnKeys,
            DatabaseEndpoint staging,
            String stagingSchema,
            String stagingTableName,
            List<String> physicalColumnNames,
            int fetchSize
    ) {
        long rowNo = 0;
        long inserted = 0;
        try (SourceTableBatchReader reader = sourceDataReaderPort.openTableReader(
                source, sourceSchema, tableName, sourceColumnKeys, fetchSize
        )) {
            while (true) {
                List<Map<String, Object>> rows = reader.readNextBatch();
                if (rows.isEmpty()) {
                    break;
                }
                inserted += insertStagingBatch(
                        staging, stagingSchema, stagingTableName, sourceColumnKeys, physicalColumnNames, rows, rowNo
                );
                rowNo += rows.size();
            }
        }
        return inserted;
    }

    @Override
    public long loadFromSql(
            DatabaseEndpoint source,
            ValidatedExtractQuery query,
            List<String> sourceColumnKeys,
            DatabaseEndpoint staging,
            String stagingSchema,
            String stagingTableName,
            List<String> physicalColumnNames,
            int fetchSize
    ) {
        long rowNo = 0;
        long inserted = 0;
        try {
            Connection connection = jdbcConnectionProvider.openConnection(source);
            try (SqlStreamingBatchReader reader = new SqlStreamingBatchReader(
                    connection, source.vendor(), query, sourceColumnKeys, fetchSize
            )) {
                while (true) {
                    List<Map<String, Object>> rows = reader.readNextBatch();
                    if (rows.isEmpty()) {
                        break;
                    }
                    inserted += insertStagingBatch(
                            staging, stagingSchema, stagingTableName, sourceColumnKeys, physicalColumnNames, rows, rowNo
                    );
                    rowNo += rows.size();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("원천 SQL 로드에 실패했습니다.", exception);
        }
        return inserted;
    }

    @Override
    public DedupResult deduplicate(
            DatabaseEndpoint staging,
            String schemaName,
            String rawTableName,
            String finalTableName
    ) {
        StagingMybatisGateway.DedupCounts counts = stagingMybatisGateway.deduplicate(
                staging, schemaName, rawTableName, finalTableName
        );
        return new DedupResult(counts.rawCount(), counts.finalCount(), counts.duplicateCount());
    }

    private long insertStagingBatch(
            DatabaseEndpoint staging,
            String stagingSchema,
            String stagingTableName,
            List<String> sourceColumnKeys,
            List<String> physicalColumnNames,
            List<Map<String, Object>> rows,
            long rowNoStart
    ) {
        if (rows.isEmpty()) {
            return 0;
        }
        if (sourceColumnKeys.size() != physicalColumnNames.size()) {
            throw new IllegalArgumentException(
                    "원천 컬럼 수와 스테이징 물리 컬럼 수가 일치하지 않습니다: source="
                            + sourceColumnKeys.size() + ", physical=" + physicalColumnNames.size()
            );
        }
        List<String> columns = new ArrayList<>();
        columns.add("__row_no");
        columns.add("__row_hash");
        columns.addAll(physicalColumnNames);
        List<List<Object>> payloadRows = new ArrayList<>(rows.size());
        long rowNo = rowNoStart;
        for (Map<String, Object> row : rows) {
            rowNo++;
            List<String> values = sourceColumnKeys.stream()
                    .map(key -> stringify(row.get(key)))
                    .toList();
            String hash = JdbcSqlDialect.rowHash(values.toArray(String[]::new));
            List<Object> payloadRow = new ArrayList<>(2 + values.size());
            payloadRow.add(rowNo);
            payloadRow.add(hash);
            payloadRow.addAll(values);
            payloadRows.add(payloadRow);
        }
        return stagingMybatisGateway.insertRows(
                staging, stagingSchema, stagingTableName, columns, payloadRows
        );
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static final class SqlStreamingBatchReader implements AutoCloseable {
        private final PreparedStatement statement;
        private final ResultSet resultSet;
        private final List<String> columnKeys;
        private final int batchSize;
        private boolean finished;

        private SqlStreamingBatchReader(
                Connection connection,
                DatabaseVendor vendor,
                ValidatedExtractQuery query,
                List<String> columnKeys,
                int batchSize
        ) throws SQLException {
            this.columnKeys = columnKeys;
            this.batchSize = batchSize;
            String sql = JdbcSqlDialect.driverSql(vendor, query.sql());
            statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            int timeoutSeconds = (int) Math.min(Integer.MAX_VALUE, query.timeout().getSeconds());
            if (timeoutSeconds > 0) {
                statement.setQueryTimeout(timeoutSeconds);
            }
            if (query.maxRows() > 0) {
                statement.setMaxRows(query.maxRows());
            }
            if (vendor == DatabaseVendor.ORACLE) {
                statement.setFetchSize(Math.min(Math.max(batchSize, 100), 10_000));
            } else if (vendor == DatabaseVendor.MYSQL || vendor == DatabaseVendor.MARIADB) {
                statement.setFetchSize(Integer.MIN_VALUE);
            } else {
                statement.setFetchSize(batchSize);
            }
            resultSet = statement.executeQuery();
        }

        private List<Map<String, Object>> readNextBatch() throws SQLException {
            if (finished) {
                return List.of();
            }
            List<Map<String, Object>> rows = new ArrayList<>(batchSize);
            while (rows.size() < batchSize && resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 0; i < columnKeys.size(); i++) {
                    row.put(columnKeys.get(i), JdbcSqlDialect.copyJdbcValue(resultSet.getObject(i + 1)));
                }
                rows.add(row);
            }
            if (rows.isEmpty()) {
                finished = true;
            }
            return rows;
        }

        @Override
        public void close() throws SQLException {
            resultSet.close();
            statement.close();
        }
    }
}
