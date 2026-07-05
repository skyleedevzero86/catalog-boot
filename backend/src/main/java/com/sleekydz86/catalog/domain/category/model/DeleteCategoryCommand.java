package com.sleekydz86.catalog.domain.category.model;

public record DeleteCategoryCommand(
        String categoryId,
        String actorId
) {
}
