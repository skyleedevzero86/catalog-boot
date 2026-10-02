package com.sleekydz86.catalog.adapter.outbound.fileload;

import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcConnectionProvider;
import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcSqlDialect;
import com.sleekydz86.catalog.adapter.outbound.staging.StagingMybatisGateway;
import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import com.sleekydz86.catalog.domain.fileload.model.FileColumnDef;
import com.sleekydz86.catalog.domain.fileload.port.out.FileLoadTargetPort;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.global.exception.BusinessException;
import com.sleekydz86.catalog.global.exception.ErrorCode;
import com.sleekydz86.catalog.global.exception.InfrastructureException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class FileLoadTargetAdapter implements FileLoadTargetPort {

    private static final int CLICKHOUSE_VALUES_CHUNK = 2_000;

    private final StagingMybatisGateway stagingMybatisGateway;
    private final JdbcConnectionProvider jdbcConnectionProvider;

    public FileLoadTargetAdapter(
            StagingMybatisGateway stagingMybatisGateway,
            JdbcConnectionProvider jdbcConnectionProvider
    ) {
        this.stagingMybatisGateway = stagingMybatisGateway;
        this.jdbcConnectionProvider = jdbcConnectionProvider;
    }

    @Override
    public void createTable(
            DatabaseEndpoint target,
            String schemaName,
            String tableName,
            String tableComment,
            List<FileColumnDef> columns
    ) {
        try {
            if (target.vendor() == DatabaseVendor.CLICKHOUSE) {
                createClickHouseTable(target, schemaName, tableName, columns);
            } else if (stagingMybatisGateway.supportsStoredProcedures(target)) {
                stagingMybatisGateway.createTable(
                        target,
                        schemaName,
                        tableName,
                        buildColumnsDdl(target.vendor(), columns)
                );
                applyComments(target, schemaName, tableName, tableComment, columns);
                applyMysqlTableComment(target, schemaName, tableName, tableComment);
            } else {
                throw new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "지원하지 않는 파일 적재 벤더입니다. vendor=" + target.vendor()
                );
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw InfrastructureException.of(
                    ErrorCode.FILE_LOAD_FAILED,
                    "테이블 생성에 실패했습니다. vendor=" + target.vendor()
                            + " table=" + qualified(target.vendor(), schemaName, tableName),
                    exception
            );
        }
    }

    @Override
    public long insertRows(
            DatabaseEndpoint target,
            String schemaName,
            String tableName,
            List<String> columnNames,
            List<List<Object>> rows
    ) {
        try {
            if (target.vendor() == DatabaseVendor.CLICKHOUSE) {
                return clickHouseMultiValuesInsert(target, schemaName, tableName, columnNames, rows);
            }
            if (stagingMybatisGateway.supportsStoredProcedures(target)) {
                return stagingMybatisGateway.insertRows(target, schemaName, tableName, columnNames, rows);
            }
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "지원하지 않는 파일 적재 벤더입니다. vendor=" + target.vendor()
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw InfrastructureException.of(
                    ErrorCode.FILE_LOAD_FAILED,
                    "파일 데이터 적재에 실패했습니다. vendor=" + target.vendor()
                            + " table=" + qualified(target.vendor(), schemaName, tableName),
                    exception
            );
        }
    }

    private void createClickHouseTable(
            DatabaseEndpoint target,
            String schemaName,
            String tableName,
            List<FileColumnDef> columns
    ) {
        String ddl = "CREATE TABLE IF NOT EXISTS " + qualified(DatabaseVendor.CLICKHOUSE, schemaName, tableName)
                + " (" + buildColumnsDdl(DatabaseVendor.CLICKHOUSE, columns) + ")"
                + " ENGINE = MergeTree ORDER BY tuple()";
        jdbcConnectionProvider.runWithRetry(target, connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(ddl);
            } catch (SQLException exception) {
                throw new InfrastructureException(
                        ErrorCode.FILE_LOAD_FAILED,
                        "ClickHouse 테이블 생성에 실패했습니다. table="
                                + qualified(DatabaseVendor.CLICKHOUSE, schemaName, tableName),
                        exception
                );
            }
        });
    }

    private long clickHouseMultiValuesInsert(
            DatabaseEndpoint target,
            String schemaName,
            String tableName,
            List<String> columnNames,
            List<List<Object>> rows
    ) {
        DatabaseVendor vendor = DatabaseVendor.CLICKHOUSE;
        String colList = columnNames.stream()
                .map(name -> quoteIdent(vendor, name))
                .collect(Collectors.joining(", "));
        String prefix = "INSERT INTO " + qualified(vendor, schemaName, tableName)
                + " (" + colList + ") VALUES ";
        return jdbcConnectionProvider.executeWithRetry(target, connection -> {
            try (Statement statement = connection.createStatement()) {
                long count = 0;
                StringBuilder values = new StringBuilder();
                int inChunk = 0;
                for (List<Object> row : rows) {
                    if (inChunk > 0) {
                        values.append(',');
                    }
                    values.append('(');
                    for (int i = 0; i < columnNames.size(); i++) {
                        if (i > 0) {
                            values.append(',');
                        }
                        values.append(clickHouseLiteral(i < row.size() ? row.get(i) : null));
                    }
                    values.append(')');
                    inChunk++;
                    count++;
                    if (inChunk >= CLICKHOUSE_VALUES_CHUNK) {
                        statement.execute(prefix + values);
                        values.setLength(0);
                        inChunk = 0;
                    }
                }
                if (inChunk > 0) {
                    statement.execute(prefix + values);
                }
                return count;
            } catch (SQLException exception) {
                throw new InfrastructureException(
                        ErrorCode.FILE_LOAD_FAILED,
                        "ClickHouse multi-VALUES INSERT 실패 table="
                                + qualified(vendor, schemaName, tableName),
                        exception
                );
            }
        });
    }

    private String clickHouseLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String text) {
            if (text.isBlank()) {
                return "NULL";
            }
            return "'" + text.replace("\\", "\\\\").replace("'", "\\'") + "'";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "'" + String.valueOf(value).replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private void applyMysqlTableComment(
            DatabaseEndpoint target,
            String schemaName,
            String tableName,
            String tableComment
    ) {
        if (tableComment == null || tableComment.isBlank()) {
            return;
        }
        if (target.vendor() != DatabaseVendor.MYSQL && target.vendor() != DatabaseVendor.MARIADB) {
            return;
        }
        String sql = "ALTER TABLE " + qualified(target.vendor(), schemaName, tableName)
                + " COMMENT=" + quoteLiteral(tableComment);
        jdbcConnectionProvider.runWithRetry(target, connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException exception) {
                throw new InfrastructureException(
                        ErrorCode.FILE_LOAD_FAILED,
                        "MySQL/MariaDB 테이블 코멘트 설정에 실패했습니다. table="
                                + qualified(target.vendor(), schemaName, tableName),
                        exception
                );
            }
        });
    }

    private void applyComments(
            DatabaseEndpoint target,
            String schemaName,
            String tableName,
            String tableComment,
            List<FileColumnDef> columns
    ) {
        if (target.vendor() != DatabaseVendor.POSTGRESQL && target.vendor() != DatabaseVendor.ORACLE) {
            return;
        }
        boolean hasTableComment = tableComment != null && !tableComment.isBlank();
        boolean hasColumnComment = columns.stream()
                .anyMatch(column -> column.comment() != null && !column.comment().isBlank());
        if (!hasTableComment && !hasColumnComment) {
            return;
        }
        DatabaseVendor vendor = target.vendor();
        jdbcConnectionProvider.runWithRetry(target, connection -> {
            try (Statement statement = connection.createStatement()) {
                if (hasTableComment) {
                    statement.execute("COMMENT ON TABLE " + qualified(vendor, schemaName, tableName)
                            + " IS " + quoteLiteral(tableComment));
                }
                for (FileColumnDef column : columns) {
                    if (column.comment() == null || column.comment().isBlank()) {
                        continue;
                    }
                    statement.execute("COMMENT ON COLUMN " + qualified(vendor, schemaName, tableName)
                            + "." + quoteIdent(vendor, column.name())
                            + " IS " + quoteLiteral(column.comment()));
                }
            } catch (SQLException exception) {
                throw new InfrastructureException(
                        ErrorCode.FILE_LOAD_FAILED,
                        "테이블/컬럼 코멘트 설정에 실패했습니다. table="
                                + qualified(vendor, schemaName, tableName),
                        exception
                );
            }
        });
    }

    private String buildColumnsDdl(DatabaseVendor vendor, List<FileColumnDef> columns) {
        return columns.stream()
                .map(column -> buildOneColumnDdl(vendor, column))
                .collect(Collectors.joining(", "));
    }

    private String buildOneColumnDdl(DatabaseVendor vendor, FileColumnDef column) {
        String type = normalizeSqlType(vendor, column.sqlType());
        if (vendor == DatabaseVendor.CLICKHOUSE) {
            String chType = column.nullable() ? "Nullable(" + type + ")" : type;
            return quoteIdent(vendor, column.name()) + " " + chType;
        }
        String notNull = column.nullable() ? "" : " NOT NULL";
        String comment = "";
        if ((vendor == DatabaseVendor.MYSQL || vendor == DatabaseVendor.MARIADB)
                && column.comment() != null && !column.comment().isBlank()) {
            comment = " COMMENT " + quoteLiteral(column.comment());
        }
        return quoteIdent(vendor, column.name()) + " " + type + notNull + comment;
    }

    private String normalizeSqlType(DatabaseVendor vendor, String sqlType) {
        String upper = sqlType.trim().toUpperCase(Locale.ROOT);
        return switch (vendor) {
            case ORACLE -> normalizeOracleType(upper, sqlType);
            case MYSQL, MARIADB -> normalizeMysqlType(upper, sqlType);
            case CLICKHOUSE -> normalizeClickHouseType(upper, sqlType);
            case POSTGRESQL -> normalizePostgresType(upper, sqlType);
        };
    }

    private String normalizePostgresType(String upper, String original) {
        return switch (upper) {
            case "STRING", "TEXT", "VARCHAR" -> "VARCHAR(255)";
            case "INT", "INTEGER" -> "INTEGER";
            case "LONG", "BIGINT" -> "BIGINT";
            case "DOUBLE", "FLOAT", "NUMERIC", "DECIMAL" -> "NUMERIC(18,4)";
            case "BOOL", "BOOLEAN" -> "BOOLEAN";
            case "DATE" -> "DATE";
            case "TIMESTAMP", "DATETIME" -> "TIMESTAMP";
            default -> original.trim();
        };
    }

    private String normalizeOracleType(String upper, String original) {
        return switch (upper) {
            case "STRING", "TEXT", "VARCHAR", "VARCHAR2" -> "VARCHAR2(255)";
            case "INT", "INTEGER" -> "NUMBER(10)";
            case "LONG", "BIGINT" -> "NUMBER(19)";
            case "DOUBLE", "FLOAT", "NUMERIC", "DECIMAL" -> "NUMBER(18,4)";
            case "BOOL", "BOOLEAN" -> "NUMBER(1)";
            case "DATE" -> "DATE";
            case "TIMESTAMP", "DATETIME" -> "TIMESTAMP";
            default -> original.trim();
        };
    }

    private String normalizeMysqlType(String upper, String original) {
        return switch (upper) {
            case "STRING", "TEXT", "VARCHAR" -> "VARCHAR(255)";
            case "INT", "INTEGER" -> "INT";
            case "LONG", "BIGINT" -> "BIGINT";
            case "DOUBLE", "FLOAT", "NUMERIC", "DECIMAL" -> "DECIMAL(18,4)";
            case "BOOL", "BOOLEAN" -> "TINYINT(1)";
            case "DATE" -> "DATE";
            case "TIMESTAMP", "DATETIME" -> "DATETIME";
            default -> original.trim();
        };
    }

    private String normalizeClickHouseType(String upper, String original) {
        return switch (upper) {
            case "STRING", "TEXT", "VARCHAR" -> "String";
            case "INT", "INTEGER" -> "Int32";
            case "LONG", "BIGINT" -> "Int64";
            case "DOUBLE", "FLOAT", "NUMERIC", "DECIMAL" -> "Float64";
            case "BOOL", "BOOLEAN" -> "UInt8";
            case "DATE" -> "Date";
            case "TIMESTAMP", "DATETIME" -> "DateTime";
            default -> original.trim();
        };
    }

    private String qualified(DatabaseVendor vendor, String schemaName, String tableName) {
        return JdbcSqlDialect.qualifiedName(vendor, schemaName, tableName);
    }

    private String quoteIdent(DatabaseVendor vendor, String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "식별자가 비어 있습니다.");
        }
        if (!value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "식별자는 영문/숫자/밑줄만 허용합니다: " + value
            );
        }
        return JdbcSqlDialect.quoteIdentifier(vendor, value);
    }

    private String quoteLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
