package com.sleekydz86.catalog.adapter.inbound.web.metadata;

import com.sleekydz86.catalog.adapter.outbound.persistence.codetype.CodeTypeSummaryRow;
import com.sleekydz86.catalog.adapter.outbound.persistence.metadata.MetaTableListRow;
import com.sleekydz86.catalog.global.application.MetadataQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/meta")
@Tag(name = "02-메타데이터 조회")
public class MetadataQueryController {

    private final MetadataQueryService metadataQueryService;

    public MetadataQueryController(MetadataQueryService metadataQueryService) {
        this.metadataQueryService = metadataQueryService;
    }

    @GetMapping("/tables/{mtdtId}")
    @Operation(
            summary = "메타데이터 테이블 목록 조회",
            description = "메타데이터세트에 등록된 테이블 목록을 반환합니다. (`v_mtdt_tbl_list` 뷰)"
    )
    @ApiResponse(
            responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MetaTableListRow.class)))
    )
    public List<MetaTableListRow> listTables(
            @Parameter(description = "메타데이터세트 ID", example = "mtdt-20260623-001")
            @PathVariable String mtdtId
    ) {
        return metadataQueryService.listTables(mtdtId);
    }

    @GetMapping("/code-types/{mtdtId}")
    @Operation(
            summary = "등록된 코드유형 목록 조회",
            description = "코드 테이블로 등록 완료된 코드유형 목록입니다."
    )
    @ApiResponse(
            responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CodeTypeSummaryRow.class)))
    )
    public List<CodeTypeSummaryRow> listCodeTypes(
            @Parameter(description = "메타데이터세트 ID", example = "mtdt-20260623-001")
            @PathVariable String mtdtId
    ) {
        return metadataQueryService.listCodeTypes(mtdtId);
    }

    @GetMapping("/code-types/{mtdtId}/candidates")
    @Operation(
            summary = "코드유형 등록 후보 목록 조회",
            description = "코드 테이블로 지정 가능하지만 아직 등록되지 않은 테이블 후보 목록입니다."
    )
    @ApiResponse(
            responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CodeTypeSummaryRow.class)))
    )
    public List<CodeTypeSummaryRow> listCodeTypeCandidates(
            @Parameter(description = "메타데이터세트 ID", example = "mtdt-20260623-001")
            @PathVariable String mtdtId
    ) {
        return metadataQueryService.listCodeTypeCandidates(mtdtId);
    }
}
