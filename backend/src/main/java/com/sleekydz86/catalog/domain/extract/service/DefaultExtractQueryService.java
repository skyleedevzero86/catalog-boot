package com.sleekydz86.catalog.domain.extract.service;

import com.sleekydz86.catalog.extract.model.ExtractQueryCommand;
import com.sleekydz86.catalog.domain.extract.model.ValidatedExtractQuery;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultExtractQueryService implements ExtractQueryService {

    private final ExtractSqlValidator sqlValidator = new ExtractSqlValidator();

    @Override
    public ValidatedExtractQuery build(ExtractQueryCommand command, int maxRows, Duration timeout) {
        boolean hasSql = command.generatedSql() != null && !command.generatedSql().isBlank();
        boolean hasTable = command.tableName() != null && !command.tableName().isBlank();
        if (!hasSql && !hasTable) {
            throw new IllegalArgumentException("generatedSql 또는 tableName 중 하나는 필수입니다.");
        }
        if (hasSql) {
            if (!command.clientGeneratedSqlEnabled()) {
                throw new IllegalArgumentException("클라이언트 generatedSql 경로는 비활성화되어 있습니다. tableName을 사용하세요.");
            }
            sqlValidator.validateReadOnlySelect(command.generatedSql());
            return new ValidatedExtractQuery(
                    command.generatedSql().trim(),
                    List.of(),
                    maxRows,
                    timeout
            );
        }
        String columnList = command.sourceColumnKeys().stream()
                .map(key -> ExtractSqlBuilder.quoteIdentifier(command.vendor(), key))
                .collect(Collectors.joining(", "));
        String qualifiedTable = ExtractSqlBuilder.qualifiedTable(
                command.vendor(),
                command.sourceSchema(),
                command.tableName()
        );
        String sql = "SELECT " + columnList + " FROM " + qualifiedTable;
        return new ValidatedExtractQuery(sql, List.of(), maxRows, timeout);
    }
}
