package com.sleekydz86.catalog.adapter.outbound.jdbc;

mport org.springframework.stereotype.Component;

import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.global.config.MigrationJdbcProperties;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Component
public class JdbcSourceDataReaderAdapter implements SourceDataReaderPort {

    private final JdbcConnectionProvider jdbcConnectionProvider;
    private final MigrationJdbcProperties migrationJdbcProperties;

    public JdbcSourceDataReaderAdapter(
            JdbcConnectionProvider jdbcConnectionProvider,
            MigrationJdbcProperties migrationJdbcProperties
    ) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
        this.migrationJdbcProperties = migrationJdbcProperties;
    }

    @Override
    public SourceTableBatchReader openTableReader(
            DatabaseEndpoint source,
            String schemaName,
            String tableName,
            List<String> columnNames,
            int batchSize
    ) {
        try {
            Connection connection = jdbcConnectionProvider.openConnection(source);
            return JdbcSourceTableBatchReaders.open(
                    connection,
                    source,
                    schemaName,
                    tableName,
                    columnNames,
                    batchSize,
                    migrationJdbcProperties.sourceFetchSize()
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("원천 DB 연결에 실패했습니다.", exception);
        }
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
        try (SourceTableBatchReader reader = openOffsetReader(
                source, schemaName, tableName, columnNames, batchSize, offset
        )) {
            return reader.readNextBatch();
        }
    }

    private SourceTableBatchReader openOffsetReader(
            DatabaseEndpoint source,
            String schemaName,
            String tableName,
            List<String> columnNames,
            int batchSize,
            int offset
    ) {
        try {
            Connection connection = jdbcConnectionProvider.openConnection(source);
            return JdbcSourceTableBatchReaders.openOffsetPaged(
                    connection, source, schemaName, tableName, columnNames, batchSize, offset
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("원천 DB 연결에 실패했습니다.", exception);
        }
    }
}
