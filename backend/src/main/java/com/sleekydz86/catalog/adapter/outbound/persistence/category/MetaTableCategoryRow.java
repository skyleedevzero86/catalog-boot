package com.sleekydz86.catalog.adapter.outbound.persistence.category;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class MetaTableCategoryRow {
    private String mtdtTblCtgrId;
    private String mtdtId;
    private String upMtdtTblCtgrId;
    private String ctgrNm;
    private String ctgrExpln;
    private Integer sortNo;
    private Boolean expsrYn;
    private Boolean lwrCtgrPrmYn;
    private String creatrId;
    private String mdfrId;
    private Instant crtDt;
    private Instant mdfcnDt;
}
