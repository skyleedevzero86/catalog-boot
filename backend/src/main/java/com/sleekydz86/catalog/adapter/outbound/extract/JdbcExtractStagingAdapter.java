package com.sleekydz86.catalog.adapter.outbound.extract;

import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcConnectionProvider;
import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcSqlDialect;
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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JdbcExtractStagingAdapter implements ExtractStagingPort {

    private final JdbcConnectionProvider jdbcConnectionProvider;
    private final SourceDataReaderPort sourceDataReaderPort;

    public JdbcExtractStagingAdapter(
            JdbcConnectionProvider jdbcConnectionProvider,
            SourceDataReaderPort sourceDataReaderPort
    ) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
        this.sourceDataReaderPort = sourceDataReaderPort;
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
        jdbcConnectionProvider.runWithRetry(staging, connection -> {
            String qualified = JdbcSqlDialect.qualifiedName(staging.vendor(), schemaName, staging.schemaName(), tableName);
            String sql = switch (staging.vendor()) {
                case POSTGRESQL -> "DROP TABLE IF EXISTS " + qualified + " CASCADE";
                case MYSQL, MARIADB, CLICKHOUSE -> "DROP TABLE IF EXISTS " + qualified;
                case ORACLE -> "BEGIN EXECUTE IMMEDIATE 'DROP TABLE " + qualified
                        + "'; EXCEPTION WHEN OTHERS THEN NULL; END;";
            };
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        });
    }

    @Override
    public void createRawStagingTable(
            DatabaseEndpoint staging,
            String schemaName,
            String tableName,
            List<String> physicalColumnNames
    ) {
        String valueType = JdbcSqlDialect.stagingValueType(staging.vendor());
        StringBuilder body = new StringBuilder();
        body.append(JdbcSqlDialect.quoteIdentifier(staging.vendor(), "__row_no"))
                .append(" BIGINT NOT NULL,\n");
        body.append(JdbcSqlDialect.quoteIdentifier(staging.vendor(), "__row_hash"))
                .append(" VARCHAR(64),\n");
        for (String column : physicalColumnNames) {
            body.append(JdbcSqlDialect.quoteIdentifier(staging.vendor(), column))
                    .append(" ").append(valueType).append(",\n");
        }
        body.setLength(body.length() - 2);

        String qualified = JdbcSqlDialect.qualifiedName(staging.vendor(), schemaName, staging.schemaName(), tableName);
        String ddl = switch (staging.vendor()) {
            case CLICKHOUSE -> "CREATE TABLE " + qualified + " (\n" + body + "\n) ENGINE = MergeTree() ORDER BY __row_no";
            default -> "CREATE TABLE " + qualified + " (\n" + body + "\n)";
        };
        jdbcConnectionProvider.runWithRetry(staging, connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(ddl);
            }
        });
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
                inserted += insertStagingBatch(staging, stagingSchema, stagingTableName, physicalColumnNames, rows, rowNo);
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
                    inserted += insertStagingBatch(staging, stagingSchema, stagingTableName, physicalColumnNames, rows, rowNo);
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
        String rawQualified = JdbcSqlDialect.qualifiedName(staging.vendor(), schemaName, staging.schemaName(), rawTableName);
        String finalQualified = JdbcSqlDialect.qualifiedName(staging.vendor(), schemaName, staging.schemaName(), finalTableName);
        long rawCount = countRows(staging, rawQualified);
        String sql = dedupSql(staging.vendor(), rawQualified, finalQualified);
        jdbcConnectionProvider.runWithRetry(staging, connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        });
        long finalCount = countRows(staging, finalQualified);
        return new DedupResult(rawCount, finalCount, rawCount - finalCount);
    }

    private long insertStagingBatch(
            DatabaseEndpoint staging,
            String stagingSchema,
            String stagingTableName,
            List<String> physicalColumnNames,
            List<Map<String, Object>> rows,
            long rowNoStart
    ) {
        if (rows.isEmpty()) {
            return 0;
        }
        String qualified = JdbcSqlDialect.qualifiedName(staging.vendor(), stagingSchema, staging.schemaName(), stagingTableName);
        List<String> columns = new ArrayList<>();
        columns.add("__row_no");
        columns.add("__row_hash");
        columns.addAll(physicalColumnNames);
        String columnList = columns.stream()
                .map(name -> JdbcSqlDialect.quoteIdentifier(staging.vendor(), name))
                .collect(Collectors.joining(", "));
        String placeholders = columns.stream().map(ignored -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + qualified + " (" + columnList + ") VALUES (" + placeholders + ")";

        return jdbcConnectionProvider.executeWithRetry(staging, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                long rowNo = rowNoStart;
                for (Map<String, Object> row : rows) {
                    rowNo++;
                    List<String> values = physicalColumnNames.stream()
                            .map(key -> stringify(row.get(key)))
                            .toList();
                    String hash = JdbcSqlDialect.rowHash(values.toArray(String[]::new));
                    statement.setLong(1, rowNo);
                    statement.setString(2, hash);
                    for (int i = 0; i < values.size(); i++) {
                        statement.setString(i + 3, values.get(i));
                    }
                    statement.addBatch();
                }
                statement.executeBatch();
                return rows.size();
            }
        });
    }

    private long countRows(DatabaseEndpoint staging, String qualifiedTable) {
        return jdbcConnectionProvider.executeWithRetry(staging, connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + qualifiedTable)) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        });
    }

    private String dedupSql(DatabaseVendor vendor, String rawQualified, String finalQualified) {
        return switch (vendor) {
            case POSTGRESQL -> "CREATE TABLE " + finalQualified + " AS SELECT DISTINCT ON (__row_hash) * FROM "
                    + rawQualified + " ORDER BY __row_hash, __row_no";
            case MYSQL, MARIADB -> "CREATE TABLE " + finalQualified + " AS SELECT r.* FROM " + rawQualified + " r INNER JOIN ("
                    + "SELECT __row_hash, MIN(__row_no) AS mn FROM " + rawQualified + " GROUP BY __row_hash"
                    + ") d ON r.__row_hash = d.__row_hash AND r.__row_no = d.mn";
            case ORACLE -> "CREATE TABLE " + finalQualified + " AS SELECT r.* FROM " + rawQualified + " r WHERE r.__row_no IN ("
                    + "SELECT MIN(__row_no) FROM " + rawQualified + " GROUP BY __row_hash)";
            case CLICKHOUSE -> "CREATE TABLE " + finalQualified + " ENGINE = MergeTree() ORDER BY __row_no AS SELECT * FROM "
                    + rawQualified + " LIMIT 1 BY __row_hash";
        };
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
