package com.sleekydz86.catalog.adapter.outbound.persistence.migration;


import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class MigrationJobTableRow {
    private String migJobTblId;
    private String migJobId;
    private String tblNm;
    private String tblSttsCd;
    private Long rowCnt;
    private Integer batchCnt;
    private String crtTblDdlCn;
    private String errMsgCn;
    private Instant bgngDt;
    private Instant endDt;
    private Integer sortNo;
    private Instant crtDt;
    private Instant mdfcnDt;
}
