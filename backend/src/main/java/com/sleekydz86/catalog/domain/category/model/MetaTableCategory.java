package com.sleekydz86.catalog.domain.category.model;


public record MetaTableCategory(
        String id,
        String mtdtId,
        String parentId,
        String name,
        String description,
        int sortNo,
        boolean exposed,
        boolean allowChildCategories,
        String creatorId,
        String modifierId,
        boolean deleted
) {

    public static MetaTableCategory createNew(
            String mtdtId,
            String parentId,
            String name,
            String description,
            int sortNo,
            boolean exposed,
            boolean allowChildCategories,
            String actorId
    ) {
        return new MetaTableCategory(
                null,
                mtdtId,
                parentId,
                name,
                description,
                sortNo,
                exposed,
                allowChildCategories,
                actorId,
                actorId,
                false
        );
    }

    public MetaTableCategory withUpdate(
            String parentId,
            String name,
            String description,
            Integer sortNo,
            Boolean exposed,
            Boolean allowChildCategories,
            String actorId
    ) {
        return new MetaTableCategory(
                id,
                mtdtId,
                parentId == null ? this.parentId : parentId,
                name == null ? this.name : name,
                description == null ? this.description : description,
                sortNo == null ? this.sortNo : sortNo,
                exposed == null ? this.exposed : exposed,
                allowChildCategories == null ? this.allowChildCategories : allowChildCategories,
                creatorId,
                actorId,
                false
        );
    }

    public MetaTableCategory markDeleted(String actorId) {
        return new MetaTableCategory(
                id, mtdtId, parentId, name, description, sortNo, exposed, allowChildCategories,
                creatorId, actorId, true
        );
    }
}
