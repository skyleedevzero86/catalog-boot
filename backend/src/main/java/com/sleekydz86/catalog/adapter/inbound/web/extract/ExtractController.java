package com.sleekydz86.catalog.adapter.inbound.web.extract;

import com.sleekydz86.catalog.domain.extract.model.CleanupExtractCommand;
import com.sleekydz86.catalog.domain.extract.model.ExportExtractCommand;
import com.sleekydz86.catalog.domain.extract.model.ExportExtractResult;
import com.sleekydz86.catalog.domain.extract.model.ExtractCodeMappingSpec;
import com.sleekydz86.catalog.domain.extract.model.ExtractColumnSpec;
import com.sleekydz86.catalog.domain.extract.model.ExtractDatasetManifest;
import com.sleekydz86.catalog.domain.extract.model.ExtractDatasetStatus;
import com.sleekydz86.catalog.domain.extract.model.ExtractJobStatus;
import com.sleekydz86.catalog.domain.extract.model.PrepareExtractCommand;
import com.sleekydz86.catalog.domain.extract.model.PrepareExtractResult;
import com.sleekydz86.catalog.domain.extract.model.CancelExtractRequestCommand;
import com.sleekydz86.catalog.domain.extract.model.SubmitExtractRequestCommand;
import com.sleekydz86.catalog.domain.extract.service.ExtractRequestCommandService;
import com.sleekydz86.catalog.global.application.ExtractApplicationService;
import com.sleekydz86.catalog.global.config.openapi.OpenApiResponses;
import com.sleekydz86.catalog.global.security.AuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/extract")
@Tag(name = "06-추출(Extract)")
public class ExtractController {

    private final ExtractRequestCommandService extractRequestCommandService;
    private final ExtractApplicationService extractApplicationService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ExtractController(
            ExtractRequestCommandService extractRequestCommandService,
            ExtractApplicationService extractApplicationService,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.extractRequestCommandService = extractRequestCommandService;
        this.extractApplicationService = extractApplicationService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping("/prepare")
    @Operation(
            summary = "추출 데이터셋 준비 (스테이징·코드매핑·중복제거)",
            description = """
                    원천 DB에서 데이터를 읽어 **스테이징 DB**(등록된 연결, PG/MySQL/Oracle/CH 지원)에 적재합니다.
                    - `deduplicate=true` 시 `__row_hash` 기준 중복 제거
                    - `codeMappings`로 코드명 매핑 테이블 생성
                    """
    )
    @OpenApiResponses
    public PrepareExtractResponse prepare(
            @RequestBody PrepareRequest request
    ) {
        PrepareExtractResult result = extractApplicationService.prepare(new PrepareExtractCommand(
                request.datasetId(),
                request.sourceConnectionId(),
                request.stagingConnectionId(),
                request.sourceSchema(),
                request.tableName(),
                request.generatedSql(),
                request.columns(),
                request.codeMappings(),
                request.deduplicate(),
                request.replaceExisting(),
                request.fetchSize(),
                authenticatedUserProvider.currentUserId()
        ));
        return PrepareExtractResponse.from(result);
    }

    @PostMapping("/datasets/{datasetId}/export")
    @Operation(summary = "준비된 데이터셋을 CSV 파일로 추출")
    @OpenApiResponses
    public ExportExtractResponse exportDataset(
            @PathVariable String datasetId,
            @RequestBody(required = false) ExportRequest request
    ) {
        ExportRequest body = request == null ? new ExportRequest(null, true, "csv", true, List.of(), null) : request;
        ExportExtractResult result = extractApplicationService.export(new ExportExtractCommand(
                datasetId,
                body.outputPath(),
                body.includeHeader() == null || body.includeHeader(),
                body.outputFormat(),
                body.singleFile() == null || body.singleFile(),
                body.selectedColumnKeys(),
                body.maxRowsPerFile(),
                authenticatedUserProvider.currentUserId()
        ));
        return ExportExtractResponse.from(result);
    }

    @PostMapping("/datasets/{datasetId}/cleanup")
    @Operation(summary = "스테이징·매핑 테이블 정리")
    @OpenApiResponses
    public DatasetResponse cleanup(
            @PathVariable String datasetId,
            @RequestBody(required = false) CleanupRequest request
    ) {
        boolean dropManifest = request != null && Boolean.TRUE.equals(request.dropManifest());
        extractApplicationService.cleanup(new CleanupExtractCommand(
                datasetId,
                dropManifest,
                authenticatedUserProvider.currentUserId()
        ));
        return DatasetResponse.from(extractApplicationService.getManifest(datasetId));
    }

    @GetMapping("/datasets/{datasetId}")
    @Operation(summary = "추출 데이터셋 manifest 조회")
    @OpenApiResponses
    public DatasetResponse dataset(@PathVariable String datasetId) {
        return DatasetResponse.from(extractApplicationService.getManifest(datasetId));
    }

    @PostMapping("/submit")
    @Operation(
            summary = "추출 작업 제출 (레거시)",
            description = "Worker HTTP 연동용 레거시 API. 내장 파이프라인은 `/extract/prepare` 사용."
    )
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ExtractResponse.class)))
    public ExtractResponse submit(
            @RequestBody SubmitRequest request
    ) {
        String jobId = extractRequestCommandService.handle(new SubmitExtractRequestCommand(
                request.mtdtId(),
                request.tableName(),
                authenticatedUserProvider.currentUserId()
        ));
        return new ExtractResponse(jobId, extractRequestCommandService.getStatus(jobId));
    }

    @PostMapping("/cancel/{extractRequestId}")
    @Operation(summary = "추출 작업 취소 (레거시 Worker)")
    @OpenApiResponses
    public ExtractResponse cancel(
            @Parameter(description = "추출 요청 ID") @PathVariable String extractRequestId
    ) {
        extractRequestCommandService.handle(new CancelExtractRequestCommand(
                extractRequestId,
                authenticatedUserProvider.currentUserId()
        ));
        return new ExtractResponse(extractRequestId, extractRequestCommandService.getStatus(extractRequestId));
    }

    @GetMapping("/status/{extractRequestId}")
    @Operation(summary = "추출 작업 상태 조회 (레거시 Worker)")
    @OpenApiResponses
    public ExtractResponse status(@PathVariable String extractRequestId) {
        return new ExtractResponse(extractRequestId, extractRequestCommandService.getStatus(extractRequestId));
    }

    @Schema(name = "ExtractPrepareRequest")
    public record PrepareRequest(
            String datasetId,
            @NotBlank String sourceConnectionId,
            String stagingConnectionId,
            String sourceSchema,
            String tableName,
            String generatedSql,
            @NotEmpty List<ExtractColumnSpec> columns,
            List<ExtractCodeMappingSpec> codeMappings,
            Boolean deduplicate,
            Boolean replaceExisting,
            Integer fetchSize
    ) {
    }

    @Schema(name = "ExtractPrepareResponse")
    public record PrepareExtractResponse(
            String datasetId,
            ExtractDatasetStatus status,
            long rowCount,
            long duplicateCount,
            String stagingTableName,
            List<String> mappingTableNames
    ) {
        static PrepareExtractResponse from(PrepareExtractResult result) {
            return new PrepareExtractResponse(
                    result.datasetId(),
                    result.status(),
                    result.rowCount(),
                    result.duplicateCount(),
                    result.stagingTableName(),
                    result.mappingTableNames()
            );
        }
    }

    @Schema(name = "ExtractExportRequest")
    public record ExportRequest(
            String outputPath,
            Boolean includeHeader,
            String outputFormat,
            Boolean singleFile,
            List<String> selectedColumnKeys,
            Integer maxRowsPerFile
    ) {
    }

    @Schema(name = "ExtractExportResponse")
    public record ExportExtractResponse(
            String datasetId,
            ExtractDatasetStatus status,
            long rowCount,
            List<String> filePaths
    ) {
        static ExportExtractResponse from(ExportExtractResult result) {
            return new ExportExtractResponse(
                    result.datasetId(),
                    result.status(),
                    result.rowCount(),
                    result.filePaths()
            );
        }
    }

    @Schema(name = "ExtractCleanupRequest")
    public record CleanupRequest(Boolean dropManifest) {
    }

    @Schema(name = "ExtractDatasetResponse")
    public record DatasetResponse(
            String datasetId,
            ExtractDatasetStatus status,
            long rowCount,
            long duplicateCount,
            String stagingTableName,
            List<String> mappingTableNames,
            List<String> exportFilePaths
    ) {
        static DatasetResponse from(ExtractDatasetManifest manifest) {
            return new DatasetResponse(
                    manifest.datasetId(),
                    manifest.status(),
                    manifest.rowCount(),
                    manifest.duplicateCount(),
                    manifest.stagingTableName(),
                    manifest.mappingTableNames(),
                    manifest.exportFilePaths()
            );
        }
    }

    @Schema(name = "ExtractSubmitRequest")
    public record SubmitRequest(
            @NotBlank String mtdtId,
            @NotBlank String tableName
    ) {
    }

    @Schema(name = "ExtractLegacyResponse")
    public record ExtractResponse(String extractRequestId, ExtractJobStatus status) {
    }
}
