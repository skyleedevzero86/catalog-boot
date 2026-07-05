package com.sleekydz86.catalog.global.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sleekydz86.catalog.adapter.outbound.persistence.codetype.CodeTypeQueryMapper;
import com.sleekydz86.catalog.adapter.outbound.persistence.codetype.CodeTypeSummaryRow;
import com.sleekydz86.catalog.adapter.outbound.persistence.metadata.MetaTableListRow;
import com.sleekydz86.catalog.adapter.outbound.persistence.metadata.MetaTableQueryMapper;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MetadataQueryService {

    private final MetaTableQueryMapper metaTableQueryMapper;
    private final CodeTypeQueryMapper codeTypeQueryMapper;

    public MetadataQueryService(MetaTableQueryMapper metaTableQueryMapper, CodeTypeQueryMapper codeTypeQueryMapper) {
        this.metaTableQueryMapper = metaTableQueryMapper;
        this.codeTypeQueryMapper = codeTypeQueryMapper;
    }

    public List<MetaTableListRow> listTables(String mtdtId) {
        return metaTableQueryMapper.selectTableList(mtdtId);
    }

    public List<CodeTypeSummaryRow> listCodeTypes(String mtdtId) {
        return codeTypeQueryMapper.selectCodeTypeList(mtdtId);
    }

    public List<CodeTypeSummaryRow> listCodeTypeCandidates(String mtdtId) {
        return codeTypeQueryMapper.selectCodeTypeCandidates(mtdtId);
    }
}
