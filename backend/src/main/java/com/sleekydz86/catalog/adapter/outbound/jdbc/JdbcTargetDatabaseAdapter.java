package com.sleekydz86.catalog.adapter.outbound.jdbc;

import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.migration.port.out.TargetDatabasePort;
import org.springframework.stereotype.Component;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JdbcTargetDatabaseAdapter implements TargetDatabasePort {

    private final JdbcConnectionProvider jdbcConnectionProvider;

    public JdbcTargetDatabaseAdapter(JdbcConnectionProvider jdbcConnectionProvider) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
    }

    @Override
    public void executeDdl(DatabaseEndpoint target, String ddl) {
        jdbcConnectionProvider.runWithRetry(target, connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(ddl);
            }
        });
    }

    @Override
    public void dropTableIfExists(DatabaseEndpoint target, String schemaName, String tableName) {
        String qualified = JdbcSqlDialect.qualifiedName(target.vendor(), schemaName, target.schemaName(), tableName);
        String sql = dropClause(target.vendor(), qualified);
        jdbcConnectionProvider.runWithRetry(target, connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        });
    }

    @Override
    public long batchInsert(
            DatabaseEndpoint target,
            String schemaName,
            String tableName,
            List<String> columnNames,
            List<List<Object>> rows
    ) {
        if (rows.isEmpty()) {
            return 0;
        }
        String qualified = JdbcSqlDialect.qualifiedName(target.vendor(), schemaName, target.schemaName(), tableName);
        String columns = columnNames.stream()
                .map(name -> JdbcSqlDialect.quoteIdentifier(target.vendor(), name))
                .collect(Collectors.joining(", "));
        String placeholders = columnNames.stream().map(ignored -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + qualified + " (" + columns + ") VALUES (" + placeholders + ")";

        return jdbcConnectionProvider.executeWithRetry(target, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (List<Object> row : rows) {
                    for (int i = 0; i < row.size(); i++) {
                        statement.setObject(i + 1, row.get(i));
                    }
                    statement.addBatch();
                }
                int[] counts = statement.executeBatch();
                long inserted = 0;
                for (int count : counts) {
                    if (count > 0) {
                        inserted += count;
                    }
                }
                return inserted == 0 ? rows.size() : inserted;
            }
        });
    }

    private String dropClause(DatabaseVendor vendor, String qualifiedTable) {
        return switch (vendor) {
            case POSTGRESQL -> "DROP TABLE IF EXISTS " + qualifiedTable + " CASCADE";
            case MYSQL, MARIADB -> "DROP TABLE IF EXISTS " + qualifiedTable;
            case ORACLE -> JdbcSqlDialect.oracleDropTablePlSql(qualifiedTable);
            case CLICKHOUSE -> "DROP TABLE IF EXISTS " + qualifiedTable;
        };
    }
}
