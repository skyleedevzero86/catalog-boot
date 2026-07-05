package com.sleekydz86.catalog.domain.category.model;

public record UpdateCategoryCommand(
        String categoryId,
        String parentId,
        String name,
        String description,
        Integer sortNo,
        Boolean exposed,
        Boolean allowChildCategories,
        String actorId
) {
}
