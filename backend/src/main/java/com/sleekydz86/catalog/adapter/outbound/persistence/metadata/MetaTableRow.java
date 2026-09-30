package com.sleekydz86.catalog.adapter.outbound.persistence.metadata;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class MetaTableRow {
    private String mtdtTblId;
    private String mtdtId;
    private String orgnlTblNm;
    private String tblNm;
    private String tblExpln;
    private String orgnlTblExpln;
    private Integer sortNo;
    private Boolean expsrYn;
    private Boolean srcExstYn;
    private Boolean useYn;
    private Boolean cdTblYn;
    private String tblTypeCd;
    private String creatrId;
    private String mdfrId;
    private Instant crtDt;
    private Instant mdfcnDt;
}
