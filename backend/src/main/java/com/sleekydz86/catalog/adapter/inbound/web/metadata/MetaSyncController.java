package com.sleekydz86.catalog.adapter.inbound.web.metadata;


import com.sleekydz86.catalog.domain.metadata.model.MetaSyncStatus;
import com.sleekydz86.catalog.domain.metadata.model.SyncMetadataCommand;
import com.sleekydz86.catalog.global.application.MetaSyncApplicationService;
import com.sleekydz86.catalog.global.config.openapi.OpenApiResponses;
import com.sleekydz86.catalog.global.config.openapi.UserIdHeader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/meta")
@Tag(name = "03-메타데이터 동기화")
public class MetaSyncController {

    private final MetaSyncApplicationService metaSyncApplicationService;

    public MetaSyncController(MetaSyncApplicationService metaSyncApplicationService) {
        this.metaSyncApplicationService = metaSyncApplicationService;
    }

    @PostMapping("/sync")
    @Operation(
            summary = "메타데이터 동기화",
            description = """
                    원천 DB JDBC `DatabaseMetaData`로 테이블 목록을 introspection하여 `t_mtdt_tbl`에 반영합니다.
                    - 신규 테이블: `sp_mtdt_tbl` op=`C`
                    - 원천에서 사라진 테이블: `src_exst_yn=false`
                    - 다시 나타난 테이블: 복원
                    - `t_mtdt_set.last_sync_*` 갱신
                    """
    )
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MetaSyncWebDto.SyncResponse.class)))
    public MetaSyncWebDto.SyncResponse sync(
            @RequestBody MetaSyncWebDto.SyncRequest request,
            @UserIdHeader @RequestHeader(value = "userId", required = false) String userIdHeader
    ) {
        var result = metaSyncApplicationService.sync(new SyncMetadataCommand(
                request.mtdtId(),
                actor(userIdHeader)
        ));
        return new MetaSyncWebDto.SyncResponse(
                result.mtdtId(),
                result.lastSyncStatus(),
                result.lastSyncAt(),
                result.lastSyncMessage(),
                result.tableCount(),
                result.addedTableCount(),
                result.missingTableCount(),
                result.restoredTableCount()
        );
    }

    private String actor(String userIdHeader) {
        return StringUtils.hasText(userIdHeader) ? userIdHeader.trim() : "system";
    }

    public static final class MetaSyncWebDto {

        private MetaSyncWebDto() {
        }

        @Schema(name = "MetaSyncRequest", description = "메타데이터 동기화 요청")
        public record SyncRequest(
                @Schema(description = "동기화할 메타데이터세트 ID", example = "mtdt-20260623-001")
                @NotBlank String mtdtId
        ) {
        }

        @Schema(name = "MetaSyncResponse", description = "메타데이터 동기화 결과")
        public record SyncResponse(
                String mtdtId,
                @Schema(description = "동기화 상태")
                MetaSyncStatus lastSyncStatus,
                Instant lastSyncAt,
                String lastSyncMessage,
                @Schema(description = "전체 등록 테이블 수")
                int tableCount,
                @Schema(description = "이번 동기화에서 추가된 테이블 수")
                int addedTableCount,
                @Schema(description = "원천에서 사라진 테이블 수")
                int missingTableCount,
                @Schema(description = "복원된 테이블 수")
                int restoredTableCount
        ) {
        }
    }
}
