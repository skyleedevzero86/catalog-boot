package com.sleekydz86.catalog.adapter.outbound.persistence.metadata;

import com.sleekydz86.catalog.domain.metadata.model.MetaSet;
import com.sleekydz86.catalog.domain.metadata.model.MetaSyncStatus;
import com.sleekydz86.catalog.domain.metadata.model.MetaTable;

final class MetaPersistenceMapper {

    private MetaPersistenceMapper() {
    }

    static MetaSet toMetaSetDomain(MetaSetRow row) {
        if (row == null) {
            return null;
        }
        return new MetaSet(
                row.getMtdtId(),
                row.getLnkgId(),
                row.getMtdtNm(),
                row.getMtdtExpln(),
                row.getLastSyncDt(),
                MetaSyncStatus.valueOf(row.getLastSyncSttsCd()),
                row.getLastSyncMsgCn(),
                row.getCreatrId(),
                row.getMdfrId(),
                false
        );
    }

    static MetaTable toMetaTableDomain(MetaTableRow row) {
        if (row == null) {
            return null;
        }
        return new MetaTable(
                row.getMtdtTblId(),
                row.getMtdtId(),
                row.getOrgnlTblNm(),
                row.getTblNm(),
                row.getTblExpln(),
                row.getOrgnlTblExpln(),
                row.getSortNo() == null ? 0 : row.getSortNo(),
                Boolean.TRUE.equals(row.getExpsrYn()),
                Boolean.TRUE.equals(row.getSrcExstYn()),
                Boolean.TRUE.equals(row.getUseYn()),
                Boolean.TRUE.equals(row.getCdTblYn()),
                row.getTblTypeCd(),
                row.getCreatrId(),
                row.getMdfrId(),
                false
        );
    }

    static java.util.Map<String, Object> toMetaSetProcedureParams(MetaSet metaSet, String op) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("op", op);
        params.put("mtdtId", metaSet.id());
        params.put("lnkgId", metaSet.connectionId());
        params.put("mtdtNm", metaSet.name());
        params.put("mtdtExpln", metaSet.description());
        params.put("lastSyncDt", metaSet.lastSyncAt());
        params.put("lastSyncSttsCd", metaSet.lastSyncStatus().name());
        params.put("lastSyncMsgCn", metaSet.lastSyncMessage());
        params.put("actorId", metaSet.modifierId());
        return params;
    }

    static java.util.Map<String, Object> toMetaTableProcedureParams(MetaTable metaTable, String op) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("op", op);
        params.put("mtdtTblId", metaTable.id());
        params.put("mtdtId", metaTable.mtdtId());
        params.put("orgnlTblNm", metaTable.originalTableName());
        params.put("tblNm", metaTable.tableName());
        params.put("tblExpln", metaTable.tableDescription());
        params.put("orgnlTblExpln", metaTable.originalTableDescription());
        params.put("sortNo", metaTable.sortNo());
        params.put("expsrYn", metaTable.exposed());
        params.put("srcExstYn", metaTable.sourceExists());
        params.put("useYn", metaTable.useYn());
        params.put("cdTblYn", metaTable.codeTable());
        params.put("tblTypeCd", metaTable.tableTypeCode());
        params.put("actorId", metaTable.modifierId());
        return params;
    }
}
