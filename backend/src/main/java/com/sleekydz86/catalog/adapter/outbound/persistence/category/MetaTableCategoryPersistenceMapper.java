package com.sleekydz86.catalog.adapter.outbound.persistence.category;


import com.sleekydz86.catalog.domain.category.model.MetaTableCategory;

final class MetaTableCategoryPersistenceMapper {

    private MetaTableCategoryPersistenceMapper() {
    }

    static MetaTableCategory toDomain(MetaTableCategoryRow row) {
        if (row == null) {
            return null;
        }
        return new MetaTableCategory(
                row.getMtdtTblCtgrId(),
                row.getMtdtId(),
                row.getUpMtdtTblCtgrId(),
                row.getCtgrNm(),
                row.getCtgrExpln(),
                row.getSortNo() == null ? 0 : row.getSortNo(),
                Boolean.TRUE.equals(row.getExpsrYn()),
                Boolean.TRUE.equals(row.getLwrCtgrPrmYn()),
                row.getCreatrId(),
                row.getMdfrId(),
                false
        );
    }

    static java.util.Map<String, Object> toCategoryProcedureParams(MetaTableCategory category, String op) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("op", op);
        params.put("mtdtTblCtgrId", category.id());
        params.put("mtdtId", category.mtdtId());
        params.put("upMtdtTblCtgrId", category.parentId());
        params.put("ctgrNm", category.name());
        params.put("ctgrExpln", category.description());
        params.put("sortNo", category.sortNo());
        params.put("expsrYn", category.exposed());
        params.put("lwrCtgrPrmYn", category.allowChildCategories());
        params.put("actorId", category.modifierId());
        return params;
    }

    static java.util.Map<String, Object> toMappingProcedureParams(
            String mtdtId,
            String categoryId,
            String tableId,
            int sortNo,
            String actorId,
            String op
    ) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("op", op);
        params.put("mtdtTblCtgrMpngId", null);
        params.put("mtdtId", mtdtId);
        params.put("mtdtTblCtgrId", categoryId);
        params.put("mtdtTblId", tableId);
        params.put("sortNo", sortNo);
        params.put("expsrYn", true);
        params.put("actorId", actorId);
        return params;
    }
}
