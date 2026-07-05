package com.sleekydz86.catalog.domain.metadata.port.out;

import com.sleekydz86.catalog.domain.metadata.model.MetaSet;
import com.sleekydz86.catalog.domain.metadata.model.MetaTable;
import java.util.List;
import java.util.Optional;

public interface MetaPersistencePort {
    MetaSet saveMetaSet(MetaSet metaSet);
    Optional<MetaSet> findMetaSetById(String mtdtId);
    MetaTable saveMetaTable(MetaTable metaTable);
    List<MetaTable> findSourceTablesByMtdtId(String mtdtId);
    Optional<MetaTable> findSourceTableByOriginalName(String mtdtId, String originalTableName);
}