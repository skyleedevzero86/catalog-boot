package com.sleekydz86.catalog.adapter.outbound.persistence.category;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MetaTableCategoryQueryMapper {

    boolean existsSiblingName(
            @Param("mtdtId") String mtdtId,
            @Param("parentId") String parentId,
            @Param("ctgrNm") String ctgrNm,
            @Param("excludeId") String excludeId
    );

    List<MetaTableCategoryMappingRow> selectCategoryTableMappings(@Param("mtdtTblCtgrId") String mtdtTblCtgrId);
}
