package com.sleekydz86.catalog.domain.connection.model;

public record UpdateConnectionCommand(
        String connectionId,
        String name,
        String host,
        Integer port,
        String databaseName,
        String schemaName,
        String description,
        String username,
        String rawPassword,
        Boolean enabled,
        String actorId
) {
}