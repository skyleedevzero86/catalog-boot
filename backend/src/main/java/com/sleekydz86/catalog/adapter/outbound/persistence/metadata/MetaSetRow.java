package com.sleekydz86.catalog.adapter.outbound.persistence.metadata;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class MetaSetRow {
    private String mtdtId;
    private String lnkgId;
    private String mtdtNm;
    private String mtdtExpln;
    private Instant lastSyncDt;
    private String lastSyncSttsCd;
    private String lastSyncMsgCn;
    private String creatrId;
    private String mdfrId;
    private Instant crtDt;
    private Instant mdfcnDt;
}
