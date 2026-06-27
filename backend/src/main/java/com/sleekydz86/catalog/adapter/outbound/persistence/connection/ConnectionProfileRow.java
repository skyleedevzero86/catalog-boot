package com.sleekydz86.catalog.adapter.outbound.persistence.connection;


import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class ConnectionProfileRow {
    private String lnkgId;
    private String lnkgNm;
    private String dbTypeCd;
    private String srvrAddr;
    private Integer srvrPortNo;
    private String dbNm;
    private String schmNm;
    private String lnkgExpln;
    private String acntId;
    private String enpswd;
    private Boolean useYn;
    private String lnkgSttsCd;
    private String testRsltCd;
    private Instant lastTestDt;
    private String creatrId;
    private String mdfrId;
}
