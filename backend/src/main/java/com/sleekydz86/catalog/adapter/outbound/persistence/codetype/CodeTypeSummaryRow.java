package com.sleekydz86.catalog.adapter.outbound.persistence.codetype;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Schema(name = "CodeTypeSummaryRow", description = "코드유형 요약 행")
@Getter
@Setter
public class CodeTypeSummaryRow {
    private String mtdtTblId;
    private String orgnlTblNm;
    private String tblNm;
    private Boolean srcExstYn;
    private Boolean cdTblYn;
    @Schema(description = "코드유형 등록 완료 여부")
    private Boolean regYn;
    @Schema(description = "코드유형 ID")
    private String cdTypeId;
    private String cdTypeNm;
    private String cdTypeExpln;
    private String cdColNm;
    private String cdNmColNm;
    private Long assignedColumnCount;
    private String mdfrId;
    private Instant mdfcnDt;
}
