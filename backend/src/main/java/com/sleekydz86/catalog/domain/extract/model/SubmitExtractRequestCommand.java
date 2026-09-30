public package com.sleekydz86.catalog.domain.extract.model;


public record SubmitExtractRequestCommand(
        String mtdtId,
        String tableName,
        String actorId
) {
}
