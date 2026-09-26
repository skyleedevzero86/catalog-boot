package com.sleekydz86.catalog.adapter.outbound.persistence.metadata;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface MetaCommandMapper {

    void executeMtdtSet(Map<String, Object> params);

    void executeMtdtTbl(Map<String, Object> params);

    MetaSetRow selectMetaSetById(@Param("mtdtId") String mtdtId);

    List<MetaTableRow> selectSourceTablesByMtdtId(@Param("mtdtId") String mtdtId);

    MetaTableRow selectSourceTableByOriginalName(
            @Param("mtdtId") String mtdtId,
            @Param("orgnlTblNm") String orgnlTblNm
    );

    MetaTableRow selectMetaTableById(@Param("mtdtTblId") String mtdtTblId);
}
