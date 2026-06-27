package com.sleekydz86.catalog.adapter.inbound.web.migration;

import com.sleekydz86.catalog.global.config.openapi.OpenApiResponses;
import com.sleekydz86.catalog.global.config.openapi.UserIdHeader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/migration")
@Tag(name = "05-DB 마이그레이션")
@Validated
public class MigrationController {

    private final MigrationApplicationService migrationApplicationService;
    private final MigrationJobApplicationService migrationJobApplicationService;

    public MigrationController(
            MigrationApplicationService migrationApplicationService,
            MigrationJobApplicationService migrationJobApplicationService
    ) {
        this.migrationApplicationService = migrationApplicationService;
        this.migrationJobApplicationService = migrationJobApplicationService;
    }

    @PostMapping("/ddl/preview")
    @Operation(
            summary = "타깃 DB CREATE TABLE DDL 미리보기",
            description = """
                    원천 DB JDBC 메타데이터를 읽어 타깃 벤더 DDL로 변환합니다.
                    실제 DDL 실행·데이터 적재는 수행하지 않습니다.
                    """
    )
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TargetDdlPreview.class)))
    public TargetDdlPreview previewDdl(@RequestBody PreviewDdlRequest request) {
        return migrationApplicationService.previewDdl(
                request.sourceConnectionId(),
                request.targetConnectionId(),
                request.sourceSchema(),
                request.targetSchema(),
                request.tableName()
        );
    }

    @PostMapping("/load")
    @Operation(
            summary = "타깃 DB 테이블 적재 (동기)",
            description = """
                    단일 테이블을 **동기**로 적재합니다.
                    1. `t_mig_job` / `t_mig_job_tbl` 이력 생성 (`sp_mig_job`, `sp_mig_job_tbl`)
                    2. 원천 메타 → DDL 변환 → 타깃 CREATE (옵션: `dropExisting`)
                    3. 배치 SELECT / INSERT 반복 후 결과 반환
                    """
    )
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = LoadTableResult.class)))
    public LoadTableResult loadTable(
            @RequestBody LoadTableRequest request,
            @UserIdHeader @RequestHeader(value = "userId", required = false) String userIdHeader
    ) {
        return migrationApplicationService.loadTable(
                request.sourceConnectionId(),
                request.targetConnectionId(),
                request.sourceSchema(),
                request.targetSchema(),
                request.tableName(),
                request.batchSize() == null ? 500 : request.batchSize(),
                request.dropExisting() == null || request.dropExisting(),
                actor(userIdHeader)
        );
    }

    @PostMapping("/load/batch")
    @Operation(
            summary = "타깃 DB 일괄 적재 (비동기)",
            description = """
                    여러 테이블을 **비동기 배치 작업**으로 등록합니다.
                    - `tableNames` 지정 시 해당 테이블만 적재
                    - `mtdtId`만 지정 시 메타데이터에 등록된 원천 테이블 목록으로 적재
                    - 즉시 `jobId` 반환, 백그라운드에서 병렬 적재 (`cdw.catalog.migration.jdbc.parallel-table-workers`)
                    """
    )
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MigrationJobResponse.class)))
    public MigrationJobResponse startBatchLoad(
            @RequestBody BatchLoadRequest request,
            @UserIdHeader @RequestHeader(value = "userId", required = false) String userIdHeader
    ) {
        MigrationJob job = migrationJobApplicationService.startBatchMigration(new StartBatchMigrationCommand(
                request.sourceConnectionId(),
                request.targetConnectionId(),
                request.mtdtId(),
                request.sourceSchema(),
                request.targetSchema(),
                request.tableNames(),
                request.batchSize() == null ? 500 : request.batchSize(),
                request.dropExisting() == null || request.dropExisting(),
                actor(userIdHeader)
        ));
        return MigrationJobResponse.from(job);
    }

    @GetMapping("/jobs")
    @Operation(summary = "마이그레이션 작업 목록 조회", description = "최근 작업을 생성일 역순으로 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MigrationJobResponse.class)))
    )
    public List<MigrationJobResponse> listJobs(
            @Parameter(description = "최대 조회 건수", example = "50")
            @RequestParam(defaultValue = "50") int limit
    ) {
        return migrationJobApplicationService.listJobs(limit).stream()
                .map(MigrationJobResponse::from)
                .toList();
    }

    @PostMapping("/jobs/{jobId}/cancel")
    @Operation(summary = "마이그레이션 작업 취소", description = "실행 중·대기 중 작업을 `CANCELLED`로 변경합니다.")
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MigrationJobResponse.class)))
    public MigrationJobResponse cancelJob(
            @Parameter(description = "작업 ID", example = "etl-20260623-001")
            @PathVariable String jobId
    ) {
        return MigrationJobResponse.from(migrationJobApplicationService.cancelJob(jobId));
    }

    @PostMapping("/jobs/{jobId}/retry")
    @Operation(
            summary = "마이그레이션 작업 재시도",
            description = """
                    실패·부분 성공 작업을 재실행합니다.
                    - `failedOnly=true`(기본): `FAILED` 테이블만 `PENDING`으로 초기화 후 재적재
                    - `failedOnly=false`: 전체 테이블 재적재
                    """
    )
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MigrationJobResponse.class)))
    public MigrationJobResponse retryJob(
            @Parameter(description = "작업 ID", example = "etl-20260623-001")
            @PathVariable String jobId,
            @Parameter(description = "실패 테이블만 재시도 여부", example = "true")
            @RequestParam(defaultValue = "true") boolean failedOnly
    ) {
        return MigrationJobResponse.from(migrationJobApplicationService.retryJob(jobId, failedOnly));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "마이그레이션 작업 상태 조회")
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MigrationJobResponse.class)))
    public MigrationJobResponse getJob(
            @Parameter(description = "작업 ID", example = "etl-20260623-001")
            @PathVariable String jobId
    ) {
        return MigrationJobResponse.from(migrationJobApplicationService.getJob(jobId));
    }

    @GetMapping("/jobs/{jobId}/tables")
    @Operation(summary = "마이그레이션 작업 테이블별 상태 조회", description = "작업에 포함된 테이블별 적재 진행 상태를 `sort_no` 순으로 반환합니다.")
    @OpenApiResponses
    @ApiResponse(
            responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MigrationJobTableResponse.class)))
    )
    public List<MigrationJobTableResponse> getJobTables(
            @Parameter(description = "작업 ID", example = "etl-20260623-001")
            @PathVariable String jobId
    ) {
        return migrationJobApplicationService.getJobTables(jobId).stream()
                .map(MigrationJobTableResponse::from)
                .toList();
    }

    private String actor(String userIdHeader) {
        return StringUtils.hasText(userIdHeader) ? userIdHeader.trim() : "system";
    }

    @Schema(name = "PreviewDdlRequest", description = "DDL 미리보기 요청")
    public record PreviewDdlRequest(
            @Schema(description = "원천 연결 ID", example = "lnkg-20260623-001")
            @NotBlank String sourceConnectionId,
            @Schema(description = "타깃 연결 ID", example = "lnkg-20260623-002")
            @NotBlank String targetConnectionId,
            @Schema(description = "원천 스키마", example = "HR")
            String sourceSchema,
            @Schema(description = "타깃 스키마", example = "cdw")
            @NotBlank String targetSchema,
            @Schema(description = "테이블명", example = "EMPLOYEES")
            @NotBlank String tableName
    ) {
    }

    @Schema(
            name = "LoadTableRequest",
            description = "단일 테이블 동기 적재 요청",
            example = """
                    {
                      "sourceConnectionId": "lnkg-20260623-001",
                      "targetConnectionId": "lnkg-20260623-002",
                      "sourceSchema": "HR",
                      "targetSchema": "cdw",
                      "tableName": "EMPLOYEES",
                      "batchSize": 500,
                      "dropExisting": true
                    }
                    """
    )
    public record LoadTableRequest(
            @NotNull @NotBlank String sourceConnectionId,
            @NotNull @NotBlank String targetConnectionId,
            String sourceSchema,
            @NotBlank String targetSchema,
            @NotBlank String tableName,
            @Schema(description = "배치 크기", example = "500", defaultValue = "500")
            Integer batchSize,
            @Schema(description = "적재 전 타깃 테이블 DROP 여부", example = "true", defaultValue = "true")
            Boolean dropExisting
    ) {
    }

    @Schema(
            name = "BatchLoadRequest",
            description = "배치 비동기 적재 요청",
            example = """
                    {
                      "sourceConnectionId": "lnkg-20260623-001",
                      "targetConnectionId": "lnkg-20260623-002",
                      "mtdtId": "mtdt-20260623-001",
                      "sourceSchema": "HR",
                      "targetSchema": "cdw",
                      "tableNames": ["EMPLOYEES", "DEPARTMENTS"],
                      "batchSize": 500,
                      "dropExisting": true
                    }
                    """
    )
    public record BatchLoadRequest(
            @NotBlank String sourceConnectionId,
            @NotBlank String targetConnectionId,
            @Schema(description = "메타데이터세트 ID (`tableNames` 대신 사용 가능)")
            String mtdtId,
            String sourceSchema,
            @NotBlank String targetSchema,
            @Schema(description = "적재할 테이블명 목록 (`mtdtId`와 택일)")
            List<String> tableNames,
            Integer batchSize,
            Boolean dropExisting
    ) {
    }

    @Schema(name = "MigrationJobResponse", description = "마이그레이션 배치 작업 상태")
    public record MigrationJobResponse(
            @Schema(description = "작업 ID", example = "etl-20260623-001")
            String jobId,
            @Schema(description = "작업 상태")
            MigrationJobStatus status,
            String sourceConnectionId,
            String targetConnectionId,
            String mtdtId,
            String sourceSchema,
            String targetSchema,
            @Schema(description = "총 테이블 수")
            int totalTableCount,
            @Schema(description = "성공 테이블 수")
            int successTableCount,
            @Schema(description = "실패 테이블 수")
            int failedTableCount,
            @Schema(description = "적재된 총 행 수")
            long totalRowCount,
            String errorMessage,
            Instant startedAt,
            Instant endedAt,
            Instant createdAt
    ) {
        static MigrationJobResponse from(MigrationJob job) {
            return new MigrationJobResponse(
                    job.jobId(),
                    job.status(),
                    job.sourceConnectionId(),
                    job.targetConnectionId(),
                    job.mtdtId(),
                    job.sourceSchema(),
                    job.targetSchema(),
                    job.totalTableCount(),
                    job.successTableCount(),
                    job.failedTableCount(),
                    job.totalRowCount(),
                    job.errorMessage(),
                    job.startedAt(),
                    job.endedAt(),
                    job.createdAt()
            );
        }
    }

    @Schema(name = "MigrationJobTableResponse", description = "마이그레이션 작업 내 테이블별 상태")
    public record MigrationJobTableResponse(
            @Schema(example = "migt-20260623-001")
            String jobTableId,
            @Schema(example = "EMPLOYEES")
            String tableName,
            MigrationTableStatus status,
            Long rowCount,
            Integer batchCount,
            String errorMessage,
            Instant startedAt,
            Instant endedAt,
            int sortOrder
    ) {
        static MigrationJobTableResponse from(MigrationJobTable table) {
            return new MigrationJobTableResponse(
                    table.jobTableId(),
                    table.tableName(),
                    table.status(),
                    table.rowCount(),
                    table.batchCount(),
                    table.errorMessage(),
                    table.startedAt(),
                    table.endedAt(),
                    table.sortOrder()
            );
        }
    }
}
