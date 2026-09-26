package com.sleekydz86.catalog.domain.migration.model;


import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;

public record PreviewTargetDdlCommand(
        DatabaseEndpoint source,
        DatabaseVendor targetVendor,
        String sourceSchema,
        String targetSchema,
        String tableName
) {
}
