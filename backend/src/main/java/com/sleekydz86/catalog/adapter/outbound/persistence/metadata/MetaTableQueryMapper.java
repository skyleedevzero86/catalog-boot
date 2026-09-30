package com.sleekydz86.catalog.adapter.outbound.persistence.metadata;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MetaTableQueryMapper {

    List<MetaTableListRow> selectTableList(@Param("mtdtId") String mtdtId);
}
