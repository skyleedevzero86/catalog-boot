package com.sleekydz86.catalog.adapter.outbound.persistence.category;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface MetaTableCategoryCommandMapper {
    void executeMtdtTblCtgr(Map<String, Object> params);
    void executeMtdtTblCtgrMpng(Map<String, Object> params);
    MetaTableCategoryRow selectCategoryById(@Param("mtdtTblCtgrId") String mtdtTblCtgrId);
    List<MetaTableCategoryRow> selectCategoriesByMtdtId(@Param("mtdtId") String mtdtId);
    long countChildCategories(@Param("mtdtTblCtgrId") String mtdtTblCtgrId);
    List<String> selectMappedTableIds(@Param("mtdtTblCtgrId") String mtdtTblCtgrId);
    void deleteMappingsByCategoryId(@Param("mtdtTblCtgrId") String mtdtTblCtgrId);
}
