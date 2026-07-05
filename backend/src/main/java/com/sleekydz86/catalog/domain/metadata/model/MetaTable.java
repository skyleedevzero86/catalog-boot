package com.sleekydz86.catalog.domain.metadata.model;


public record MetaTable(
        String id,
        String mtdtId,
        String originalTableName,
        String tableName,
        String tableDescription,
        String originalTableDescription,
        int sortNo,
        boolean exposed,
        boolean sourceExists,
        boolean useYn,
        boolean codeTable,
        String tableTypeCode,
        String creatorId,
        String modifierId,
        boolean deleted
) {

    public static final String SOURCE_TABLE_TYPE = "SOURCE";

    public static MetaTable createNew(
            String mtdtId,
            String originalTableName,
            String tableName,
            String originalTableDescription,
            int sortNo,
            String actorId
    ) {
        return new MetaTable(
                null,
                mtdtId,
                originalTableName,
                tableName,
                null,
                originalTableDescription,
                sortNo,
                true,
                true,
                true,
                false,
                SOURCE_TABLE_TYPE,
                actorId,
                actorId,
                false
        );
    }

    public MetaTable withSourceSync(
            String originalTableDescription,
            boolean sourceExists,
            boolean useYn,
            String actorId
    ) {
        return new MetaTable(
                id,
                mtdtId,
                originalTableName,
                tableName,
                tableDescription,
                originalTableDescription,
                sortNo,
                exposed,
                sourceExists,
                useYn,
                codeTable,
                tableTypeCode,
                creatorId,
                actorId,
                false
        );
    }

    public MetaTable markDeleted(String actorId) {
        return new MetaTable(
                id, mtdtId, originalTableName, tableName, tableDescription, originalTableDescription,
                sortNo, exposed, sourceExists, false, codeTable, tableTypeCode,
                creatorId, actorId, true
        );
    }
}
