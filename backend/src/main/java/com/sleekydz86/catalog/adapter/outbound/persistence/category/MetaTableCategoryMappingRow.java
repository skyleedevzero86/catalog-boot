package com.sleekydz86.catalog.adapter.outbound.persistence.category;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetaTableCategoryMappingRow {
    private String mtdtTblCtgrMpngId;
    private String mtdtTblCtgrId;
    private String mtdtTblId;
    private String orgnlTblNm;
    private String tblNm;
    private String tblExpln;
    private Integer sortNo;
    private Boolean expsrYn;
    private Boolean srcExstYn;
    private String tblTypeCd;
}