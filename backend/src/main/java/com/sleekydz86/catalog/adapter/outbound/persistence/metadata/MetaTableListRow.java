package com.sleekydz86.catalog.adapter.outbound.persistence.metadata;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Schema(name = "MetaTableListRow", description = "메타데이터 테이블 목록 행 (`v_mtdt_tbl_list`)")
@Getter
@Setter
public class MetaTableListRow {
    @Schema(description = "메타테이블 ID", example = "mtbl-20260623-001")
    private String mtdtTblId;
    @Schema(description = "메타데이터세트 ID", example = "mtdt-20260623-001")
    private String mtdtId;
    @Schema(description = "원천 테이블명", example = "EMPLOYEES")
    private String orgnlTblNm;
    @Schema(description = "표시 테이블명")
    private String tblNm;
    private String tblExpln;
    private String orgnlTblExpln;
    private Integer sortNo;
    private Boolean expsrYn;
    @Schema(description = "원천 DB 존재 여부")
    private Boolean srcExstYn;
    private Boolean useYn;
    @Schema(description = "코드 테이블 여부")
    private Boolean cdTblYn;
    @Schema(description = "테이블 유형", example = "SOURCE")
    private String tblTypeCd;
    private Long wholNocs;
    private Long chgNocs;
    private String statsSttsCd;
    private Long elpsMsCnt;
    private String creatrId;
    private String mdfrId;
    private Instant crtDt;
    private Instant mdfcnDt;
}
