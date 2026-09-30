package com.sleekydz86.catalog.adapter.outbound.persistence.extract;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface ExtractDatasetCommandMapper {

    Optional<ExtractDatasetRow> selectDatstById(@Param("extrDatstId") String extrDatstId);

    void executeExtrDmnd(Map<String, Object> params);

    void executeExtrDatst(Map<String, Object> params);

    void executeExtrExcn(Map<String, Object> params);
}
