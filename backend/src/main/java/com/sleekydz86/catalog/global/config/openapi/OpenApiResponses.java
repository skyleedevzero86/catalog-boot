package com.sleekydz86.catalog.global.config.openapi;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ProblemDetail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "요청 검증 실패 (필수값 누락, 형식 오류)",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
                responseCode = "404",
                description = "대상 리소스를 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
                responseCode = "409",
                description = "비즈니스 규칙 충돌 (중복 이름, 하위 카테고리 존재 등)",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
})
public @interface OpenApiResponses {
}
