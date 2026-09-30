package com.sleekydz86.catalog.domain.extract.model;

public record CleanupExtractCommand(
        String datasetId,
        boolean dropManifest,
        String actorId
) {
}
