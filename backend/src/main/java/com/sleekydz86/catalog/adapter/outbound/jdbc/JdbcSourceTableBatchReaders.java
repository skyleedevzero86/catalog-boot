package com.sleekydz86.catalog.adapter.outbound.jdbc;


import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.migration.port.out.SourceTableBatchReader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

final class JdbcSourceTableBatchReaders {

    private JdbcSourceTableBatchReaders() {
    }

    static SourceTableBatchReader open(
            Connection connection,
            DatabaseEndpoint source,
            String schemaName,
            String tableName,
            List<String> columnNames,
            int batchSize,
            int sourceFetchSize
    ) throws SQLException {
        return switch (source.vendor()) {
            case POSTGRESQL -> new PostgresCursorBatchReader(
                    connection, source, schemaName, tableName, columnNames, batchSize
            );
            case ORACLE, MYSQL, MARIADB, CLICKHOUSE -> new StreamingBatchReader(
                    connection, source, schemaName, tableName, columnNames, batchSize, sourceFetchSize
            );
        };
    }

    static SourceTableBatchReader openOffsetPaged(
            Connection connection,
            DatabaseEndpoint source,
            String schemaName,
            String tableName,
            List<String> columnNames,
            int batchSize,
            int offset
    ) throws SQLException {
        return new OffsetPagedBatchReader(
                connection, source, schemaName, tableName, columnNames, batchSize, offset
        );
    }

    private static String selectColumnList(DatabaseVendor vendor, List<String> columnNames) {
        return columnNames.stream()
                .map(name -> JdbcSqlDialect.quoteIdentifier(vendor, name))
                .collect(Collectors.joining(", "));
    }

    private static String baseSelect(
            DatabaseEndpoint source,
            String schemaName,
            String tableName,
            List<String> columnNames
    ) {
        String qualifiedTable = JdbcSqlDialect.qualifiedName(
                source.vendor(), schemaName, source.schemaName(), tableName
        );
        return "SELECT " + selectColumnList(source.vendor(), columnNames) + " FROM " + qualifiedTable;
    }

    private static List<Map<String, Object>> mapRows(ResultSet resultSet, List<String> columnNames) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < columnNames.size(); i++) {
                row.put(columnNames.get(i), JdbcSqlDialect.copyJdbcValue(resultSet.getObject(i + 1)));
            }
            rows.add(row);
        }
        return rows;
    }

    private static void configureStreamingStatement(PreparedStatement statement, DatabaseVendor vendor, int fetchSize)
            throws SQLException {
        int effectiveFetchSize = Math.max(fetchSize, 1);
        if (vendor == DatabaseVendor.ORACLE) {
            int prefetch = Math.min(Math.max(effectiveFetchSize, 100), 10_000);
            statement.setFetchSize(prefetch);
            return;
        }
        if (vendor == DatabaseVendor.MYSQL || vendor == DatabaseVendor.MARIADB) {
            statement.setFetchSize(Integer.MIN_VALUE);
            return;
        }
        statement.setFetchSize(effectiveFetchSize);
    }

    private abstract static class AbstractBatchReader implements SourceTableBatchReader {
        protected final Connection connection;
        protected final List<String> columnNames;
        protected final int batchSize;
        protected long rowsRead;
        protected boolean finished;

        protected AbstractBatchReader(Connection connection, List<String> columnNames, int batchSize) {
            this.connection = connection;
            this.columnNames = columnNames;
            this.batchSize = batchSize;
        }

        @Override
        public long rowsRead() {
            return rowsRead;
        }

        @Override
        public void close() {
            finished = true;
            try {
                connection.close();
            } catch (SQLException exception) {
                throw new IllegalStateException("원천 DB 연결 종료에 실패했습니다.", exception);
            }
        }
    }

    private static final class PostgresCursorBatchReader extends AbstractBatchReader {
        private final String cursorName;
        private boolean declared;

        PostgresCursorBatchReader(
                Connection connection,
                DatabaseEndpoint source,
                String schemaName,
                String tableName,
                List<String> columnNames,
                int batchSize
        ) throws SQLException {
            super(connection, columnNames, batchSize);
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            cursorName = JdbcSqlDialect.migrationCursorName(
                    tableName + "_" + UUID.randomUUID().toString().replace("-", "")
            );
            String declareSql = "DECLARE " + JdbcSqlDialect.quoteIdentifier(DatabaseVendor.POSTGRESQL, cursorName)
                    + " NO SCROLL CURSOR FOR " + baseSelect(source, schemaName, tableName, columnNames);
            try (Statement statement = connection.createStatement()) {
                statement.execute(declareSql);
            }
            declared = true;
        }

        @Override
        public List<Map<String, Object>> readNextBatch() {
            if (finished) {
                return List.of();
            }
            try {
                String fetchSql = "FETCH FORWARD " + batchSize + " FROM "
                        + JdbcSqlDialect.quoteIdentifier(DatabaseVendor.POSTGRESQL, cursorName);
                try (Statement statement = connection.createStatement();
                     ResultSet resultSet = statement.executeQuery(fetchSql)) {
                    List<Map<String, Object>> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 0; i < columnNames.size(); i++) {
                            row.put(columnNames.get(i), JdbcSqlDialect.copyJdbcValue(resultSet.getObject(i + 1)));
                        }
                        rows.add(row);
                    }
                    rowsRead += rows.size();
                    if (rows.isEmpty()) {
                        finished = true;
                    }
                    return rows;
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("PostgreSQL 커서에서 원천 데이터를 읽지 못했습니다.", exception);
            }
        }

        @Override
        public void close() {
            if (declared) {
                try {
                    connection.rollback();
                } catch (SQLException exception) {
                    throw new IllegalStateException("PostgreSQL 커서 트랜잭션 롤백에 실패했습니다.", exception);
                }
            }
            super.close();
        }
    }

    private static final class StreamingBatchReader extends AbstractBatchReader {
        private final PreparedStatement statement;
        private final ResultSet resultSet;

        StreamingBatchReader(
                Connection connection,
                DatabaseEndpoint source,
                String schemaName,
                String tableName,
                List<String> columnNames,
                int batchSize,
                int sourceFetchSize
        ) throws SQLException {
            super(connection, columnNames, batchSize);
            String sql = baseSelect(source, schemaName, tableName, columnNames);
            statement = connection.prepareStatement(
                    sql,
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY
            );
            configureStreamingStatement(statement, source.vendor(), Math.max(batchSize, sourceFetchSize));
            resultSet = statement.executeQuery();
        }

        @Override
        public List<Map<String, Object>> readNextBatch() {
            if (finished) {
                return List.of();
            }
            try {
                List<Map<String, Object>> rows = new ArrayList<>(batchSize);
                while (rows.size() < batchSize && resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 0; i < columnNames.size(); i++) {
                        row.put(columnNames.get(i), JdbcSqlDialect.copyJdbcValue(resultSet.getObject(i + 1)));
                    }
                    rows.add(row);
                }
                rowsRead += rows.size();
                if (rows.isEmpty()) {
                    finished = true;
                }
                return rows;
            } catch (SQLException exception) {
                throw new IllegalStateException("원천 DB 스트리밍 읽기에 실패했습니다.", exception);
            }
        }

        @Override
        public void close() {
            finished = true;
            try {
                resultSet.close();
            } catch (SQLException exception) {
                throw new IllegalStateException("원천 ResultSet 종료에 실패했습니다.", exception);
            }
            try {
                statement.close();
            } catch (SQLException exception) {
                throw new IllegalStateException("원천 PreparedStatement 종료에 실패했습니다.", exception);
            }
            super.close();
        }
    }

    private static final class OffsetPagedBatchReader extends AbstractBatchReader {
        private final DatabaseEndpoint source;
        private final String schemaName;
        private final String tableName;
        private int offset;
        private boolean singleBatch;

        OffsetPagedBatchReader(
                Connection connection,
                DatabaseEndpoint source,
                String schemaName,
                String tableName,
                List<String> columnNames,
                int batchSize,
                int offset
        ) {
            super(connection, columnNames, batchSize);
            this.source = source;
            this.schemaName = schemaName;
            this.tableName = tableName;
            this.offset = offset;
            this.singleBatch = true;
        }

        @Override
        public List<Map<String, Object>> readNextBatch() {
            if (finished || !singleBatch) {
                return List.of();
            }
            singleBatch = false;
            try {
                String qualifiedTable = JdbcSqlDialect.qualifiedName(
                        source.vendor(), schemaName, source.schemaName(), tableName
                );
                String columnList = selectColumnList(source.vendor(), columnNames);
                String sql = "SELECT " + columnList + " FROM " + qualifiedTable + limitClause(source.vendor());
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    bindLimit(statement, source.vendor(), batchSize, offset);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        List<Map<String, Object>> rows = mapRows(resultSet, columnNames);
                        rowsRead += rows.size();
                        finished = true;
                        return rows;
                    }
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("원천 DB 페이지 읽기에 실패했습니다.", exception);
            }
        }

        private static String limitClause(DatabaseVendor vendor) {
            return switch (vendor) {
                case ORACLE -> " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
                case POSTGRESQL, MYSQL, MARIADB, CLICKHOUSE -> " LIMIT ? OFFSET ?";
            };
        }

        private static void bindLimit(
                PreparedStatement statement,
                DatabaseVendor vendor,
                int batchSize,
                int offset
        ) throws SQLException {
            switch (vendor) {
                case ORACLE -> {
                    statement.setInt(1, offset);
                    statement.setInt(2, batchSize);
                }
                case POSTGRESQL, MYSQL, MARIADB, CLICKHOUSE -> {
                    statement.setInt(1, batchSize);
                    statement.setInt(2, offset);
                }
            }
        }
    }
}
