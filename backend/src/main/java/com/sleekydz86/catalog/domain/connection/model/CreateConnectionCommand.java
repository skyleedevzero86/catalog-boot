package com.sleekydz86.catalog.domain.connection.model;

public record CreateConnectionCommand(
        String name,
        DatabaseVendor vendor,
        String host,
        int port,
        String databaseName,
        String schemaName,
        String description,
        String username,
        String rawPassword,
        Boolean enabled,
        String actorId
) {
}
