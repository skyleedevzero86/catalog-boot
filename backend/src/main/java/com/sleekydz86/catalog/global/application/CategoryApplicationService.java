package com.sleekydz86.catalog.global.application;


import com.sleekydz86.catalog.adapter.outbound.persistence.category.MetaTableCategoryMappingRow;
import com.sleekydz86.catalog.adapter.outbound.persistence.category.MetaTableCategoryQueryMapper;
import com.sleekydz86.catalog.domain.category.model.*;
import com.sleekydz86.catalog.domain.category.port.out.CategoryPersistencePort;
import com.sleekydz86.catalog.domain.category.service.CategoryCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryApplicationService {

    private final CategoryCommandService categoryCommandService;
    private final CategoryPersistencePort categoryPersistencePort;
    private final MetaTableCategoryQueryMapper categoryQueryMapper;

    public CategoryApplicationService(
            CategoryCommandService categoryCommandService,
            CategoryPersistencePort categoryPersistencePort,
            MetaTableCategoryQueryMapper categoryQueryMapper
    ) {
        this.categoryCommandService = categoryCommandService;
        this.categoryPersistencePort = categoryPersistencePort;
        this.categoryQueryMapper = categoryQueryMapper;
    }

    @Transactional
    public MetaTableCategory create(CreateCategoryCommand command) {
        return categoryCommandService.handle(command);
    }

    @Transactional
    public MetaTableCategory update(UpdateCategoryCommand command) {
        return categoryCommandService.handle(command);
    }

    @Transactional
    public void delete(DeleteCategoryCommand command) {
        categoryCommandService.handle(command);
    }

    @Transactional
    public List<String> mapTables(MapCategoryTableCommand command) {
        return categoryCommandService.handle(command);
    }

    public List<MetaTableCategory> list(String mtdtId) {
        return categoryPersistencePort.findCategoriesByMtdtId(mtdtId);
    }

    public List<MetaTableCategoryMappingRow> listMappings(String categoryId) {
        return categoryQueryMapper.selectCategoryTableMappings(categoryId);
    }
}