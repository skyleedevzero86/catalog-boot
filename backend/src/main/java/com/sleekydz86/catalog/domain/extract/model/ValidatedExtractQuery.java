package com.sleekydz86.catalog.domain.extract.model;

import java.time.Duration;
import java.util.List;

public record ValidatedExtractQuery(
        String sql,
        List<Object> parameters,
        int maxRows,
        Duration timeout
) {
    public ValidatedExtractQuery {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }
}
