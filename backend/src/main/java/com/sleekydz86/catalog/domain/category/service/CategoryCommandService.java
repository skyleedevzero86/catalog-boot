package com.sleekydz86.catalog.domain.category.service;

import com.sleekydz86.catalog.domain.category.model.*;
import com.sleekydz86.catalog.domain.category.port.out.CategoryPersistencePort;
import com.sleekydz86.catalog.domain.metadata.port.out.MetaPersistencePort;
import com.sleekydz86.catalog.global.exception.ResourceConflictException;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CategoryCommandService {

    private final CategoryPersistencePort categoryPersistencePort;
    private final MetaPersistencePort metaPersistencePort;

    public CategoryCommandService(
            CategoryPersistencePort categoryPersistencePort,
            MetaPersistencePort metaPersistencePort
    ) {
        this.categoryPersistencePort = categoryPersistencePort;
        this.metaPersistencePort = metaPersistencePort;
    }

    public MetaTableCategory handle(CreateCategoryCommand command) {
        requireMetaSet(command.mtdtId());
        String parentId = trimToNull(command.parentId());
        if (parentId != null) {
            MetaTableCategory parent = requireCategory(parentId);
            assertSameMetaSet(parent.mtdtId(), command.mtdtId());
            assertChildCategoryAllowed(parent);
        }

        String name = requireName(command.name());
        assertSiblingNameUnique(command.mtdtId(), parentId, name, null);

        MetaTableCategory created = MetaTableCategory.createNew(
                command.mtdtId().trim(),
                parentId,
                name,
                trimToNull(command.description()),
                resolveSortNo(command.mtdtId(), parentId, command.sortNo()),
                command.exposed() == null || command.exposed(),
                command.allowChildCategories() == null || command.allowChildCategories(),
                command.actorId()
        );
        return categoryPersistencePort.saveCategory(created);
    }

    public MetaTableCategory handle(UpdateCategoryCommand command) {
        MetaTableCategory existing = requireCategory(command.categoryId());
        List<MetaTableCategory> allCategories = categoryPersistencePort.findCategoriesByMtdtId(existing.mtdtId());

        String parentId = command.parentId() == null
                ? existing.parentId()
                : trimToNull(command.parentId());

        if (Objects.equals(existing.id(), parentId)) {
            throw new IllegalArgumentException("카테고리는 자기 자신을 상위 카테고리로 지정할 수 없습니다.");
        }

        if (parentId != null) {
            MetaTableCategory parent = requireCategory(parentId);
            assertSameMetaSet(parent.mtdtId(), existing.mtdtId());
            assertChildCategoryAllowed(parent);
            assertNotDescendant(existing.id(), parentId, allCategories);
        }

        String name = requireName(command.name());
        assertSiblingNameUnique(existing.mtdtId(), parentId, name, existing.id());

        boolean allowChildren = command.allowChildCategories() == null
                ? existing.allowChildCategories()
                : command.allowChildCategories();
        if (!allowChildren && categoryPersistencePort.countChildCategories(existing.id()) > 0) {
            throw new IllegalArgumentException("하위 카테고리가 존재하면 하위 카테고리 허용 여부를 비활성화할 수 없습니다.");
        }

        MetaTableCategory updated = existing.withUpdate(
                parentId,
                name,
                command.description() == null ? existing.description() : trimToNull(command.description()),
                command.sortNo(),
                command.exposed(),
                allowChildren,
                command.actorId()
        );
        return categoryPersistencePort.saveCategory(updated);
    }

    public void handle(DeleteCategoryCommand command) {
        MetaTableCategory existing = requireCategory(command.categoryId());
        categoryPersistencePort.saveCategory(existing.markDeleted(command.actorId()));
    }

    public List<String> handle(MapCategoryTableCommand command) {
        MetaTableCategory category = requireCategory(command.categoryId());
        List<String> tableIds = command.tableIds() == null ? List.of() : command.tableIds();
        validateMappedTables(category.mtdtId(), tableIds);
        categoryPersistencePort.replaceCategoryMappings(
                category.mtdtId(),
                category.id(),
                tableIds,
                command.actorId()
        );
        return tableIds;
    }

    private void requireMetaSet(String mtdtId) {
        metaPersistencePort.findMetaSetById(mtdtId)
                .orElseThrow(() -> new ResourceNotFoundException("메타데이터 세트를 찾을 수 없습니다: " + mtdtId));
    }

    private MetaTableCategory requireCategory(String categoryId) {
        return categoryPersistencePort.findCategoryById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다: " + categoryId));
    }

    private void assertSameMetaSet(String expectedMtdtId, String actualMtdtId) {
        if (!Objects.equals(expectedMtdtId, actualMtdtId)) {
            throw new IllegalArgumentException("메타데이터 세트가 일치하지 않습니다.");
        }
    }

    private void assertChildCategoryAllowed(MetaTableCategory parent) {
        if (!parent.allowChildCategories()) {
            throw new IllegalArgumentException("하위 카테고리를 허용하지 않는 카테고리입니다.");
        }
    }

    private void assertSiblingNameUnique(String mtdtId, String parentId, String name, String excludeId) {
        if (categoryPersistencePort.existsSiblingName(mtdtId, parentId, name, excludeId)) {
            throw new ResourceConflictException("동일한 카테고리명이 이미 존재합니다: " + name);
        }
    }

    private void assertNotDescendant(String categoryId, String parentId, List<MetaTableCategory> categories) {
        Set<String> descendants = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(categoryId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            for (MetaTableCategory category : categories) {
                if (Objects.equals(current, category.parentId()) && descendants.add(category.id())) {
                    queue.add(category.id());
                }
            }
        }
        if (descendants.contains(parentId)) {
            throw new IllegalArgumentException("하위 카테고리를 상위 카테고리로 지정할 수 없습니다.");
        }
    }

    private void validateMappedTables(String mtdtId, List<String> tableIds) {
        Set<String> allowed = new HashSet<>();
        metaPersistencePort.findSourceTablesByMtdtId(mtdtId).forEach(table -> {
            if (table.exposed() && table.sourceExists() && table.useYn()) {
                allowed.add(table.id());
            }
        });
        for (String tableId : tableIds) {
            if (!allowed.contains(tableId)) {
                throw new ResourceNotFoundException("카테고리에 할당할 수 없는 테이블입니다: " + tableId);
            }
        }
    }

    private int resolveSortNo(String mtdtId, String parentId, Integer requested) {
        if (requested != null) {
            return requested;
        }
        return categoryPersistencePort.findCategoriesByMtdtId(mtdtId).stream()
                .filter(category -> Objects.equals(parentId, category.parentId()))
                .mapToInt(MetaTableCategory::sortNo)
                .max()
                .orElse(0) + 1;
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("카테고리명은 필수입니다.");
        }
        return name.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
