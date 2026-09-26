package com.sleekydz86.catalog.domain.metadata.model;

public record SyncMetadataCommand(
        String mtdtId,
        String actorId
) {
}
