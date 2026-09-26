package com.sleekydz86.catalog.adapter.outbound.persistence.category;

import com.sleekydz86.catalog.domain.category.model.MetaTableCategory;
import com.sleekydz86.catalog.domain.category.port.out.CategoryPersistencePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class MetaTableCategoryPersistenceAdapter implements CategoryPersistencePort {

    private final MetaTableCategoryCommandMapper commandMapper;
    private final MetaTableCategoryQueryMapper queryMapper;

    public MetaTableCategoryPersistenceAdapter(
            MetaTableCategoryCommandMapper commandMapper,
            MetaTableCategoryQueryMapper queryMapper
    ) {
        this.commandMapper = commandMapper;
        this.queryMapper = queryMapper;
    }

    @Override
    @Transactional
    public MetaTableCategory saveCategory(MetaTableCategory category) {
        String op = resolveCategoryOp(category);
        Map<String, Object> params = MetaTableCategoryPersistenceMapper.toCategoryProcedureParams(category, op);
        params.put("op", op);
        commandMapper.executeMtdtTblCtgr(params);
        String id = category.id() != null && !category.id().isBlank()
                ? category.id()
                : (String) params.get("mtdtTblCtgrId");
        return MetaTableCategoryPersistenceMapper.toDomain(commandMapper.selectCategoryById(id));
    }

    @Override
    public Optional<MetaTableCategory> findCategoryById(String categoryId) {
        return Optional.ofNullable(MetaTableCategoryPersistenceMapper.toDomain(commandMapper.selectCategoryById(categoryId)));
    }

    @Override
    public List<MetaTableCategory> findCategoriesByMtdtId(String mtdtId) {
        return commandMapper.selectCategoriesByMtdtId(mtdtId).stream()
                .map(MetaTableCategoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsSiblingName(String mtdtId, String parentId, String name, String excludeId) {
        return queryMapper.existsSiblingName(mtdtId, parentId, name, excludeId);
    }

    @Override
    public long countChildCategories(String categoryId) {
        return commandMapper.countChildCategories(categoryId);
    }

    @Override
    @Transactional
    public void replaceCategoryMappings(String mtdtId, String categoryId, List<String> tableIds, String actorId) {
        commandMapper.deleteMappingsByCategoryId(categoryId);
        int sortNo = 1;
        for (String tableId : tableIds) {
            Map<String, Object> params = MetaTableCategoryPersistenceMapper.toMappingProcedureParams(
                    mtdtId, categoryId, tableId, sortNo++, actorId, "C"
            );
            params.put("op", "C");
            commandMapper.executeMtdtTblCtgrMpng(params);
        }
    }

    @Override
    public List<String> findMappedTableIds(String categoryId) {
        return commandMapper.selectMappedTableIds(categoryId);
    }

    private String resolveCategoryOp(MetaTableCategory category) {
        if (category.deleted()) {
            return "D";
        }
        if (category.id() == null || category.id().isBlank()) {
            return "C";
        }
        return "U";
    }
}
