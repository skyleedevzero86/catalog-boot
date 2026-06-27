package com.sleekydz86.catalog.adapter.inbound.web.connection;

import com.sleekydz86.catalog.domain.connection.model.CreateConnectionCommand;
import com.sleekydz86.catalog.domain.connection.model.DeleteConnectionCommand;
import com.sleekydz86.catalog.domain.connection.model.UpdateConnectionCommand;
import com.sleekydz86.catalog.global.application.ConnectionApplicationService;
import com.sleekydz86.catalog.global.config.openapi.OpenApiResponses;
import com.sleekydz86.catalog.global.config.openapi.UserIdHeader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/conn")
@Tag(name = "01-DB 연결")
public class ConnectionController {

    private final ConnectionApplicationService connectionApplicationService;

    public ConnectionController(ConnectionApplicationService connectionApplicationService) {
        this.connectionApplicationService = connectionApplicationService;
    }

    @PostMapping("/create")
    @Operation(
            summary = "연결 프로필 등록",
            description = """
                    새 JDBC 연결 프로필을 등록합니다.
                    - `sp_lnkg_profile` op=`C` 호출, ID 미지정 시 `lnkg-YYYYMMDD-NNN` 자동 채번
                    - 등록 직후 JDBC 헬스체크 수행 (`cdw.catalog.connection.jdbc-health-check-enabled`)
                    - 비밀번호는 AES-GCM으로 암호화 저장
                    """
    )
    @OpenApiResponses
    @ApiResponse(
            responseCode = "200",
            description = "등록된 연결 프로필",
            content = @Content(schema = @Schema(implementation = ConnectionWebDto.ConnectionDetailResponse.class))
    )
    public ConnectionWebDto.ConnectionDetailResponse create(
            @Valid @RequestBody ConnectionWebDto.CreateConnectionRequest request,
            @UserIdHeader @RequestHeader(value = "userId", required = false) String userIdHeader
    ) {
        var profile = connectionApplicationService.create(new CreateConnectionCommand(
                request.name(),
                request.vendor(),
                request.host(),
                request.port(),
                request.databaseName(),
                request.schemaName(),
                request.description(),
                request.username(),
                request.password(),
                request.enabled(),
                actor(userIdHeader)
        ));
        return ConnectionWebDto.toDetail(profile);
    }

    @GetMapping("/list")
    @Operation(
            summary = "연결 프로필 목록 조회",
            description = "삭제(`DELETED`)되지 않은 연결 프로필을 최신 등록순으로 반환합니다. (`v_lnkg_profile`)"
    )
    @ApiResponse(
            responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConnectionWebDto.ConnectionSummaryResponse.class)))
    )
    public List<ConnectionWebDto.ConnectionSummaryResponse> list() {
        return connectionApplicationService.list().stream()
                .map(ConnectionWebDto::toSummary)
                .toList();
    }

    @GetMapping("/detail/{connectionId}")
    @Operation(summary = "연결 프로필 상세 조회", description = "연결 ID로 단건 상세를 조회합니다.")
    @OpenApiResponses
    @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = ConnectionWebDto.ConnectionDetailResponse.class))
    )
    public ConnectionWebDto.ConnectionDetailResponse get(
            @Parameter(description = "연결 ID", example = "lnkg-20260623-001")
            @PathVariable String connectionId
    ) {
        return ConnectionWebDto.toDetail(connectionApplicationService.get(connectionId));
    }

    @PostMapping("/update/{connectionId}")
    @Operation(
            summary = "연결 프로필 수정",
            description = """
                    연결 프로필을 부분 수정합니다. `sp_lnkg_profile` op=`U`.
                    요청에 없는 필드(null)는 DB 기존 값을 유지합니다.
                    비밀번호를 보내지 않으면 기존 암호화 비밀번호가 유지됩니다.
                    """
    )
    @OpenApiResponses
    @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = ConnectionWebDto.ConnectionDetailResponse.class))
    )
    public ConnectionWebDto.ConnectionDetailResponse update(
            @Parameter(description = "연결 ID", example = "lnkg-20260623-001")
            @PathVariable String connectionId,
            @Valid @RequestBody ConnectionWebDto.UpdateConnectionRequest request,
            @UserIdHeader @RequestHeader(value = "userId", required = false) String userIdHeader
    ) {
        return ConnectionWebDto.toDetail(connectionApplicationService.update(new UpdateConnectionCommand(
                connectionId,
                request.name(),
                request.host(),
                request.port(),
                request.databaseName(),
                request.schemaName(),
                request.description(),
                request.username(),
                request.password(),
                request.enabled(),
                actor(userIdHeader)
        )));
    }

    @PostMapping("/delete/{connectionId}")
    @Operation(
            summary = "연결 프로필 삭제",
            description = "논리 삭제합니다. `sp_lnkg_profile` op=`D` → `lnkg_stts_cd=DELETED`, `use_yn=false`"
    )
    @OpenApiResponses
    @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = ConnectionWebDto.ConnectionDetailResponse.class))
    )
    public ConnectionWebDto.ConnectionDetailResponse delete(
            @Parameter(description = "연결 ID", example = "lnkg-20260623-001")
            @PathVariable String connectionId,
            @UserIdHeader @RequestHeader(value = "userId", required = false) String userIdHeader
    ) {
        return ConnectionWebDto.toDetail(connectionApplicationService.delete(
                new DeleteConnectionCommand(connectionId, actor(userIdHeader))
        ));
    }

    private String actor(String userIdHeader) {
        return StringUtils.hasText(userIdHeader) ? userIdHeader.trim() : "system";
    }
}
