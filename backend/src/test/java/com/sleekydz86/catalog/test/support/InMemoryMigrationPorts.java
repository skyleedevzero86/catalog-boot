package com.sleekydz86.catalog.test.support;


import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMigrationPorts {

    private InMemoryMigrationPorts() {
    }

    public static class InMemorySourceMetadataPort implements SourceMetadataPort {
        private final Map<String, TableSchema> tables = new ConcurrentHashMap<>();

        public void register(TableSchema table) {
            tables.put(key(table.schemaName(), table.tableName()), table);
        }

        @Override
        public TableSchema readTable(DatabaseEndpoint source, String schemaName, String tableName) {
            return tables.get(key(schemaName, tableName));
        }

        @Override
        public List<SourceTableDescriptor> listTables(
                DatabaseEndpoint source,
                String schemaName
        ) {
            String prefix = schemaName == null ? "" : schemaName + ".";
            return tables.keySet().stream()
                    .filter(key -> key.startsWith(prefix) || schemaName == null)
                    .map(key -> {
                        String name = key.substring(prefix.length());
                        return new SourceTableDescriptor(name, null);
                    })
                    .toList();
        }

        private String key(String schema, String table) {
            return schema + "." + table;
        }
    }

    public static class InMemorySourceDataReaderPort implements SourceDataReaderPort {
        private final List<Map<String, Object>> rows = new ArrayList<>();

        public void seed(Map<String, Object> row) {
            rows.add(new LinkedHashMap<>(row));
        }

        @Override
        public SourceTableBatchReader openTableReader(
                DatabaseEndpoint source,
                String schemaName,
                String tableName,
                List<String> columnNames,
                int batchSize
        ) {
            return new InMemorySourceTableBatchReader(rows, batchSize);
        }

        @Override
        public List<Map<String, Object>> readRows(
                DatabaseEndpoint source,
                String schemaName,
                String tableName,
                List<String> columnNames,
                int batchSize,
                int offset
        ) {
            if (offset >= rows.size()) {
                return List.of();
            }
            int end = Math.min(offset + batchSize, rows.size());
            return new ArrayList<>(rows.subList(offset, end));
        }
    }

    private static final class InMemorySourceTableBatchReader implements SourceTableBatchReader {
        private final List<Map<String, Object>> rows;
        private final int batchSize;
        private int offset;
        private long rowsRead;

        private InMemorySourceTableBatchReader(List<Map<String, Object>> rows, int batchSize) {
            this.rows = rows;
            this.batchSize = batchSize;
        }

        @Override
        public List<Map<String, Object>> readNextBatch() {
            if (offset >= rows.size()) {
                return List.of();
            }
            int end = Math.min(offset + batchSize, rows.size());
            List<Map<String, Object>> batch = new ArrayList<>(rows.subList(offset, end));
            offset = end;
            rowsRead += batch.size();
            return batch;
        }

        @Override
        public long rowsRead() {
            return rowsRead;
        }

        @Override
        public void close() {
        }
    }

    public static class InMemoryTargetDatabasePort implements TargetDatabasePort {
        private final List<String> executedDdl = new ArrayList<>();
        private final Map<String, List<List<Object>>> insertedRows = new ConcurrentHashMap<>();

        @Override
        public void executeDdl(DatabaseEndpoint target, String ddl) {
            executedDdl.add(ddl);
        }

        @Override
        public void dropTableIfExists(DatabaseEndpoint target, String schemaName, String tableName) {
            insertedRows.remove(schemaName + "." + tableName);
        }

        @Override
        public long batchInsert(
                DatabaseEndpoint target,
                String schemaName,
                String tableName,
                List<String> columnNames,
                List<List<Object>> rows
        ) {
            insertedRows.computeIfAbsent(schemaName + "." + tableName, key -> new ArrayList<>()).addAll(rows);
            return rows.size();
        }

        public List<String> executedDdl() {
            return executedDdl;
        }

        public long rowCount(String schema, String table) {
            return insertedRows.getOrDefault(schema + "." + table, List.of()).size();
        }
    }

    public static DatabaseEndpoint mysqlEndpoint() {
        return new DatabaseEndpoint(DatabaseVendor.MYSQL, "localhost", 3306, "target", "cdw", "root", "root");
    }

    public static DatabaseEndpoint oracleEndpoint() {
        return new DatabaseEndpoint(DatabaseVendor.ORACLE, "localhost", 1521, "ORCL", "HR", "scott", "tiger");
    }

    public static TableSchema sampleOracleEmployees() {
        return new TableSchema("HR", "EMPLOYEES", List.of(
                new ColumnSchema("EMP_ID", "NUMBER", 2, 10, 0, false, true, 1),
                new ColumnSchema("EMP_NAME", "VARCHAR2", 12, 100, null, false, false, 2)
        ));
    }
}

