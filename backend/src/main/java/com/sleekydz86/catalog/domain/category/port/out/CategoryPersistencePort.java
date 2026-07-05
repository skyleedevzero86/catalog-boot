package com.sleekydz86.catalog.domain.category.port.out;

import com.sleekydz86.catalog.domain.category.model.MetaTableCategory;
import java.util.List;
import java.util.Optional;

public interface CategoryPersistencePort {
    MetaTableCategory saveCategory(MetaTableCategory category);
    Optional<MetaTableCategory> findCategoryById(String categoryId);
    List<MetaTableCategory> findCategoriesByMtdtId(String mtdtId);
    boolean existsSiblingName(String mtdtId, String parentId, String name, String excludeId);
    long countChildCategories(String categoryId);
    void replaceCategoryMappings(String mtdtId, String categoryId, List<String> tableIds, String actorId);
    List<String> findMappedTableIds(String categoryId);
}
