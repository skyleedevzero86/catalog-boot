package com.sleekydz86.catalog.adapter.outbound.persistence.codetype;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CodeTypeQueryMapper {
    List<CodeTypeSummaryRow> selectCodeTypeList(@Param("mtdtId") String mtdtId);
    List<CodeTypeSummaryRow> selectCodeTypeCandidates(@Param("mtdtId") String mtdtId);
}
