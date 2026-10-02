package com.sleekydz86.catalog.adapter.inbound.web.fileload;

import com.sleekydz86.catalog.domain.fileload.model.ConnectionProbeResult;
import com.sleekydz86.catalog.domain.fileload.model.FileColumnDef;
import com.sleekydz86.catalog.domain.fileload.model.FileLoadResult;
import com.sleekydz86.catalog.domain.fileload.model.FileTableSummary;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetFormat;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetTemplate;
import com.sleekydz86.catalog.global.application.FileLoadApplicationService;
import com.sleekydz86.catalog.global.config.openapi.OpenApiResponses;
import com.sleekydz86.catalog.global.exception.BusinessException;
import com.sleekydz86.catalog.global.exception.ErrorCode;
import com.sleekydz86.catalog.global.exception.InfrastructureException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/file-load")
@Tag(name = "07-파일 적재")
@Validated
public class FileLoadController {

    private final FileLoadApplicationService fileLoadApplicationService;

    public FileLoadController(FileLoadApplicationService fileLoadApplicationService) {
        this.fileLoadApplicationService = fileLoadApplicationService;
    }

    @PostMapping("/probe")
    @Operation(summary = "등록 연결 접속 점검")
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FileLoadWebDto.ProbeResponse.class)))
    public FileLoadWebDto.ProbeResponse probe(@RequestParam @NotBlank String connectionId) {
        ConnectionProbeResult result = fileLoadApplicationService.probe(connectionId);
        return new FileLoadWebDto.ProbeResponse(result.connectionId(), result.connected(), result.message());
    }

    @GetMapping("/tables")
    @Operation(summary = "대상 스키마 테이블 목록")
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = FileLoadWebDto.TableSummaryResponse.class))))
    public List<FileLoadWebDto.TableSummaryResponse> listTables(
            @RequestParam @NotBlank String connectionId,
            @RequestParam(required = false) String schemaName
    ) {
        return fileLoadApplicationService.listTables(connectionId, schemaName).stream()
                .map(this::toTableResponse)
                .toList();
    }

    @GetMapping("/columns")
    @Operation(summary = "테이블 컬럼 목록")
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = FileLoadWebDto.ColumnResponse.class))))
    public List<FileLoadWebDto.ColumnResponse> listColumns(
            @RequestParam @NotBlank String connectionId,
            @RequestParam(required = false) String schemaName,
            @RequestParam @NotBlank String tableName
    ) {
        return fileLoadApplicationService.listColumns(connectionId, schemaName, tableName).stream()
                .map(FileLoadWebDto.ColumnResponse::from)
                .toList();
    }

    @PostMapping("/tables")
    @Operation(summary = "테이블·컬럼·코멘트 생성")
    @OpenApiResponses
    public void createTable(@Valid @RequestBody FileLoadWebDto.CreateTableRequest request) {
        List<FileColumnDef> columns = request.columns().stream()
                .map(FileLoadWebDto.ColumnRequest::toDomain)
                .toList();
        fileLoadApplicationService.createTable(
                request.connectionId(),
                request.schemaName(),
                request.tableName(),
                request.tableComment(),
                columns
        );
    }

    @GetMapping("/template")
    @Operation(summary = "기존 테이블 기준 양식 다운로드 (csv/xlsx/xls)")
    @OpenApiResponses
    public ResponseEntity<ByteArrayResource> downloadTemplate(
            @RequestParam @NotBlank String connectionId,
            @RequestParam(required = false) String schemaName,
            @RequestParam @NotBlank String tableName,
            @RequestParam(defaultValue = "xlsx") String format
    ) {
        SpreadsheetTemplate template = fileLoadApplicationService.template(
                connectionId,
                schemaName,
                tableName,
                SpreadsheetFormat.from(format)
        );
        return toDownload(template);
    }

    @PostMapping("/template")
    @Operation(summary = "컬럼 정의 기준 양식 다운로드")
    @OpenApiResponses
    public ResponseEntity<ByteArrayResource> downloadTemplateForColumns(
            @Valid @RequestBody FileLoadWebDto.TemplateColumnsRequest request
    ) {
        List<FileColumnDef> columns = request.columns().stream()
                .map(FileLoadWebDto.ColumnRequest::toDomain)
                .toList();
        SpreadsheetTemplate template = fileLoadApplicationService.templateForColumns(
                request.tableName(),
                columns,
                SpreadsheetFormat.from(request.format())
        );
        return toDownload(template);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "양식 파일 업로드·적재")
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FileLoadWebDto.LoadResultResponse.class)))
    public FileLoadWebDto.LoadResultResponse upload(
            @RequestParam @NotBlank String connectionId,
            @RequestParam(required = false) String schemaName,
            @RequestParam @NotBlank String tableName,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestPart("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "업로드 파일이 비어 있습니다.");
        }
        SpreadsheetFormat spreadsheetFormat = SpreadsheetFormat.from(format);
        try {
            FileLoadResult result = fileLoadApplicationService.upload(
                    connectionId,
                    schemaName,
                    tableName,
                    spreadsheetFormat,
                    file.getInputStream()
            );
            return new FileLoadWebDto.LoadResultResponse(
                    result.connectionId(),
                    result.schemaName(),
                    result.tableName(),
                    result.insertedRows(),
                    result.message()
            );
        } catch (IOException exception) {
            throw InfrastructureException.of(
                    ErrorCode.FILE_LOAD_FAILED,
                    "업로드 파일을 읽을 수 없습니다. filename=" + file.getOriginalFilename(),
                    exception
            );
        }
    }

    private FileLoadWebDto.TableSummaryResponse toTableResponse(FileTableSummary summary) {
        return new FileLoadWebDto.TableSummaryResponse(summary.name(), summary.remarks());
    }

    private ResponseEntity<ByteArrayResource> toDownload(SpreadsheetTemplate template) {
        String encoded = URLEncoder.encode(template.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(template.contentType()))
                .contentLength(template.content().length)
                .body(new ByteArrayResource(template.content()));
    }
}
