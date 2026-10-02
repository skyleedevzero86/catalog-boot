package com.sleekydz86.catalog.adapter.outbound.staging;

import java.util.Map;

public interface StagingTableCommandMapper {

    void executeStgTbl(Map<String, Object> params);

    void executeStgDedup(Map<String, Object> params);

    void executeStgRows(Map<String, Object> params);
}
