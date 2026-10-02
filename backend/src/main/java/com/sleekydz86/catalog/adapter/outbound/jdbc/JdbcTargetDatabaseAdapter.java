package com.sleekydz86.catalog.adapter.outbound.jdbc;

import com.sleekydz86.catalog.adapter.outbound.staging.StagingMybatisGateway;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.migration.port.out.TargetDatabasePort;
import org.springframework.stereotype.Component;

import java.sql.Statement;
import java.util.List;

@Component
public class JdbcTargetDatabaseAdapter implements TargetDatabasePort {

    private final JdbcConnectionProvider jdbcConnectionProvider;
    private final StagingMybatisGateway stagingMybatisGateway;

    public JdbcTargetDatabaseAdapter(
            JdbcConnectionProvider jdbcConnectionProvider,
            StagingMybatisGateway stagingMybatisGateway
    ) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
        this.stagingMybatisGateway = stagingMybatisGateway;
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
        stagingMybatisGateway.dropTable(target, schemaName, tableName);
    }

    @Override
    public long batchInsert(
            DatabaseEndpoint target,
            String schemaName,
            String tableName,
            List<String> columnNames,
            List<List<Object>> rows
    ) {
        return stagingMybatisGateway.insertRows(target, schemaName, tableName, columnNames, rows);
    }
}
