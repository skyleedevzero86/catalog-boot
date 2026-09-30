package com.sleekydz86.catalog.domain.extract.model;

public record CancelExtractRequestCommand(
        String extractRequestId,
        String actorId
) {
}
