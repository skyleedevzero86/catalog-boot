package com.sleekydz86.catalog.adapter.outbound.persistence.metadata;

import com.sleekydz86.catalog.domain.metadata.model.MetaSet;
import com.sleekydz86.catalog.domain.metadata.model.MetaTable;
import com.sleekydz86.catalog.domain.metadata.port.out.MetaPersistencePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class MetaPersistenceAdapter implements MetaPersistencePort {

    private final MetaCommandMapper commandMapper;

    public MetaPersistenceAdapter(MetaCommandMapper commandMapper) {
        this.commandMapper = commandMapper;
    }

    @Override
    @Transactional
    public MetaSet saveMetaSet(MetaSet metaSet) {
        String op = resolveMetaSetOp(metaSet);
        Map<String, Object> params = MetaPersistenceMapper.toMetaSetProcedureParams(metaSet, op);
        params.put("op", op);
        commandMapper.executeMtdtSet(params);
        String id = metaSet.id() != null && !metaSet.id().isBlank()
                ? metaSet.id()
                : (String) params.get("mtdtId");
        return MetaPersistenceMapper.toMetaSetDomain(commandMapper.selectMetaSetById(id));
    }

    @Override
    public Optional<MetaSet> findMetaSetById(String mtdtId) {
        return Optional.ofNullable(MetaPersistenceMapper.toMetaSetDomain(commandMapper.selectMetaSetById(mtdtId)));
    }

    @Override
    @Transactional
    public MetaTable saveMetaTable(MetaTable metaTable) {
        String op = resolveMetaTableOp(metaTable);
        Map<String, Object> params = MetaPersistenceMapper.toMetaTableProcedureParams(metaTable, op);
        params.put("op", op);
        commandMapper.executeMtdtTbl(params);
        String id = metaTable.id() != null && !metaTable.id().isBlank()
                ? metaTable.id()
                : (String) params.get("mtdtTblId");
        return MetaPersistenceMapper.toMetaTableDomain(commandMapper.selectMetaTableById(id));
    }

    @Override
    public List<MetaTable> findSourceTablesByMtdtId(String mtdtId) {
        return commandMapper.selectSourceTablesByMtdtId(mtdtId).stream()
                .map(MetaPersistenceMapper::toMetaTableDomain)
                .toList();
    }

    @Override
    public Optional<MetaTable> findSourceTableByOriginalName(String mtdtId, String originalTableName) {
        return Optional.ofNullable(MetaPersistenceMapper.toMetaTableDomain(
                commandMapper.selectSourceTableByOriginalName(mtdtId, originalTableName)
        ));
    }

    private String resolveMetaSetOp(MetaSet metaSet) {
        if (metaSet.deleted()) {
            return "D";
        }
        if (metaSet.id() == null || metaSet.id().isBlank()) {
            return "C";
        }
        return "U";
    }

    private String resolveMetaTableOp(MetaTable metaTable) {
        if (metaTable.deleted()) {
            return "D";
        }
        if (metaTable.id() == null || metaTable.id().isBlank()) {
            return "C";
        }
        return "U";
    }
}
