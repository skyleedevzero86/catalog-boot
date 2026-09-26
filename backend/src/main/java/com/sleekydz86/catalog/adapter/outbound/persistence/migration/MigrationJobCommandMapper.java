package com.sleekydz86.catalog.adapter.outbound.persistence.migration;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface MigrationJobCommandMapper {

    void executeMigJob(Map<String, Object> params);
    void executeMigJobTbl(Map<String, Object> params);
    MigrationJobRow selectJobById(@Param("migJobId") String migJobId);
    List<MigrationJobTableRow> selectJobTables(@Param("migJobId") String migJobId);
    List<MigrationJobRow> selectJobs(@Param("limit") int limit);
    void resetFailedJobTables(@Param("migJobId") String migJobId);
}
