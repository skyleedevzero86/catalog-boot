package com.sleekydz86.catalog.domain.metadata.model;


import java.time.Instant;

public record MetaSet(
        String id,
        String connectionId,
        String name,
        String description,
        Instant lastSyncAt,
        MetaSyncStatus lastSyncStatus,
        String lastSyncMessage,
        String creatorId,
        String modifierId,
        boolean deleted
) {

    public static MetaSet createNew(
            String connectionId,
            String name,
            String description,
            String actorId
    ) {
        return new MetaSet(
                null,
                connectionId,
                name,
                description,
                null,
                MetaSyncStatus.NEVER,
                null,
                actorId,
                actorId,
                false
        );
    }

    public MetaSet withSyncStatus(MetaSyncStatus status, Instant syncedAt, String message, String actorId) {
        return new MetaSet(
                id,
                connectionId,
                name,
                description,
                syncedAt,
                status,
                message,
                creatorId,
                actorId,
                false
        );
    }

    public MetaSet markDeleted(String actorId) {
        return new MetaSet(
                id, connectionId, name, description, lastSyncAt, lastSyncStatus, lastSyncMessage,
                creatorId, actorId, true
        );
    }
}
