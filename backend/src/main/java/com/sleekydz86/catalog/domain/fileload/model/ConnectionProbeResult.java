package com.sleekydz86.catalog.domain.fileload.model;

public record ConnectionProbeResult(
        String connectionId,
        boolean connected,
        String message
) {
}
