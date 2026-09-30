package com.sleekydz86.catalog.domain.extract.model;

import java.util.List;

public record ExtractQueryCommand(
        DatabaseVendor vendor,
        String sourceSchema,
        String tableName,
        String generatedSql,
        List<String> sourceColumnKeys,
        boolean clientGeneratedSqlEnabled
) {
}
