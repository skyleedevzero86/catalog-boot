package com.sleekydz86.catalog.adapter.inbound.web.connection;

import com.sleekydz86.catalog.domain.connection.model.ConnectionHealthStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionLifecycleStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ConnectionWebDto {

    private ConnectionWebDto() {
    }

    @Schema(
            name = "CreateConnectionRequest",
            description = "연결 프로필 등록 요청",
            example = """
                    {
                      "name": "HR Oracle",
                      "vendor": "ORACLE",
                      "host": "db.example.com",
                      "port": 1521,
                      "databaseName": "ORCL",
                      "schemaName": "HR",
                      "description": "인사 원천 DB",
                      "username": "hr_user",
                      "password": "secret",
                      "enabled": true
                    }
                    """
    )
    public record CreateConnectionRequest(
            @Schema(description = "연결 표시 이름 (고유)", example = "HR Oracle", maxLength = 120)
            @NotBlank @Size(max = 120) String name,
            @Schema(description = "DB 벤더", example = "ORACLE")
            @NotNull DatabaseVendor vendor,
            @Schema(description = "호스트명 또는 IP", example = "db.example.com")
            @NotBlank String host,
            @Schema(description = "포트", example = "1521", minimum = "1", maximum = "65535")
            @NotNull @Min(1) @Max(65535) Integer port,
            @Schema(description = "데이터베이스명(SID/서비스명 등)", example = "ORCL")
            @NotBlank String databaseName,
            @Schema(description = "기본 스키마", example = "HR")
            String schemaName,
            @Schema(description = "설명")
            String description,
            @Schema(description = "접속 계정", example = "hr_user")
            @NotBlank String username,
            @Schema(description = "접속 비밀번호 (저장 시 AES-GCM 암호화)", example = "secret")
            String password,
            @Schema(description = "사용 여부", example = "true")
            Boolean enabled
    ) {
    }

    @Schema(name = "UpdateConnectionRequest", description = "연결 프로필 수정 요청 (null 필드는 기존 값 유지)")
    public record UpdateConnectionRequest(
            @Schema(description = "연결 표시 이름", example = "HR Oracle (DR)")
            String name,
            @Schema(description = "호스트", example = "db-dr.example.com")
            String host,
            @Schema(description = "포트", example = "1521")
            @Min(1) @Max(65535) Integer port,
            @Schema(description = "데이터베이스명")
            String databaseName,
            @Schema(description = "스키마")
            String schemaName,
            @Schema(description = "설명")
            String description,
            @Schema(description = "접속 계정")
            String username,
            @Schema(description = "비밀번호 변경 시에만 입력")
            String password,
            @Schema(description = "사용 여부")
            Boolean enabled
    ) {
    }

    @Schema(name = "ConnectionSummaryResponse", description = "연결 프로필 목록 항목")
    public record ConnectionSummaryResponse(
            @Schema(description = "연결 ID", example = "lnkg-20260623-001")
            String connectionId,
            @Schema(description = "연결명", example = "HR Oracle")
            String name,
            @Schema(description = "DB 벤더")
            DatabaseVendor vendor,
            @Schema(description = "호스트")
            String host,
            @Schema(description = "포트")
            Integer port,
            @Schema(description = "데이터베이스명")
            String databaseName,
            @Schema(description = "사용 여부")
            boolean enabled,
            @Schema(description = "수명주기 상태", example = "ACTIVE")
            ConnectionLifecycleStatus lifecycleStatus,
            @Schema(description = "헬스체크 결과", example = "HEALTHY")
            ConnectionHealthStatus healthStatus,
            @Schema(description = "마지막 연결 테스트 시각")
            Instant lastTestAt
    ) {
    }

    @Schema(name = "ConnectionDetailResponse", description = "연결 프로필 상세")
    public record ConnectionDetailResponse(
            @Schema(description = "연결 ID", example = "lnkg-20260623-001")
            String connectionId,
            String name,
            DatabaseVendor vendor,
            String host,
            Integer port,
            String databaseName,
            String schemaName,
            String description,
            @Schema(description = "접속 계정 (비밀번호는 응답에 포함되지 않음)")
            String username,
            boolean enabled,
            ConnectionLifecycleStatus lifecycleStatus,
            ConnectionHealthStatus healthStatus,
            Instant lastTestAt,
            @Schema(description = "등록자 ID")
            String creatorId,
            @Schema(description = "수정자 ID")
            String modifierId
    ) {
    }

    static ConnectionSummaryResponse toSummary(ConnectionProfile profile) {
        return new ConnectionSummaryResponse(
                profile.id(),
                profile.name(),
                profile.vendor(),
                profile.host(),
                profile.port(),
                profile.databaseName(),
                profile.enabled(),
                profile.lifecycleStatus(),
                profile.healthStatus(),
                profile.lastTestAt()
        );
    }

    static ConnectionDetailResponse toDetail(ConnectionProfile profile) {
        return new ConnectionDetailResponse(
                profile.id(),
                profile.name(),
                profile.vendor(),
                profile.host(),
                profile.port(),
                profile.databaseName(),
                profile.schemaName(),
                profile.description(),
                profile.username(),
                profile.enabled(),
                profile.lifecycleStatus(),
                profile.healthStatus(),
                profile.lastTestAt(),
                profile.creatorId(),
                profile.modifierId()
        );
    }
}
