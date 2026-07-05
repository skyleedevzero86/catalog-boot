package com.sleekydz86.catalog.domain.category.model;

import java.util.List;

public record MapCategoryTableCommand(
        String categoryId,
        List<String> tableIds,
        String actorId
) {
}
