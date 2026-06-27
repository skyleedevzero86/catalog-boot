package com.sleekydz86.catalog.adapter.outbound.persistence.connection;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface ConnectionCommandMapper {

    void executeLnkgProfile(Map<String, Object> params);

    ConnectionProfileRow selectById(@Param("lnkgId") String lnkgId);

    List<ConnectionProfileRow> selectActiveList();

    boolean existsByName(@Param("name") String name, @Param("excludeId") String excludeId);
}
