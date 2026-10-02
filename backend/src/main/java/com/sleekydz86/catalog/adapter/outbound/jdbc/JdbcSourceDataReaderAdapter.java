package com.sleekydz86.catalog.adapter.outbound.jdbc;

import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.migration.port.out.SourceDataReaderPort;
import com.sleekydz86.catalog.domain.migration.port.out.SourceTableBatchReader;
import com.sleekydz86.catalog.global.config.MigrationJdbcProperties;
import com.sleekydz86.catalog.global.exception.ErrorCode;
import com.sleekydz86.catalog.global.exception.InfrastructureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Component
public class JdbcSourceDataReaderAdapter implements SourceDataReaderPort {

    private static final Logger log = LoggerFactory.getLogger(JdbcSourceDataReaderAdapter.class);

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
            log.error(
                    "원천 DB 리더 오픈 실패 vendor={} host={}:{} schema={} table={}",
                    source.vendor(),
                    source.host(),
                    source.port(),
                    schemaName,
                    tableName,
                    exception
            );
            throw InfrastructureException.of(
                    ErrorCode.MIGRATION_FAILED,
                    "원천 DB 연결에 실패했습니다.",
                    exception
            );
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
            log.error(
                    "원천 DB offset 리더 오픈 실패 vendor={} host={}:{} schema={} table={} offset={}",
                    source.vendor(),
                    source.host(),
                    source.port(),
                    schemaName,
                    tableName,
                    offset,
                    exception
            );
            throw InfrastructureException.of(
                    ErrorCode.MIGRATION_FAILED,
                    "원천 DB 연결에 실패했습니다.",
                    exception
            );
        }
    }
}
