package com.sleekydz86.catalog.domain.metadata.model;

import java.time.Instant;

public record MetaSyncResult(
        String mtdtId,
        MetaSyncStatus lastSyncStatus,
        Instant lastSyncAt,
        String lastSyncMessage,
        int tableCount,
        int addedTableCount,
        int missingTableCount,
        int restoredTableCount
) {
}
