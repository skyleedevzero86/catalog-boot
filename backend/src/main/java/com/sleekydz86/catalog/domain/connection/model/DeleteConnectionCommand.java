package com.sleekydz86.catalog.domain.connection.model;

public record DeleteConnectionCommand(
        String connectionId,
        String actorId
) {
}
