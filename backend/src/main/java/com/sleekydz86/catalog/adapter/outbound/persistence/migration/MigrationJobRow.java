package com.sleekydz86.catalog.adapter.outbound.persistence.migration;


import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class MigrationJobRow {
    private String migJobId;
    private String srcLnkgId;
    private String trgtLnkgId;
    private String mtdtId;
    private String srcSchmNm;
    private String trgtSchmNm;
    private Integer batchSz;
    private Boolean dropExstYn;
    private String jobSttsCd;
    private Integer totTblCnt;
    private Integer succTblCnt;
    private Integer failTblCnt;
    private Long totRowCnt;
    private String errMsgCn;
    private Instant bgngDt;
    private Instant endDt;
    private String creatrId;
    private String mdfrId;
    private Instant crtDt;
    private Instant mdfcnDt;
}
