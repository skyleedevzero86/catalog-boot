package com.sleekydz86.catalog.domain.migration.model;

import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;

public record DatabaseEndpoint(
        DatabaseVendor vendor,
        String host,
        int port,
        String database,
        String schemaName,
        String username,
        String password
) {
}
