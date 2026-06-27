package com.sleekydz86.catalog.global.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI(@Value("${server.port:8081}") int serverPort) {
        return new OpenAPI()
                .info(new Info()
                        .title("CDW 카탈로그 제어 API")
                        .version("v1.0")
                        .description("""
                                CDW **카탈로그 제어 플레인** REST API 문서입니다.

                                ## 개요
                                - 원천·타깃 DB **연결 프로필** 관리 (AES-GCM 비밀번호 암호화)
                                - **메타데이터** 조회·동기화 (원천 DB introspection → `t_mtdt_*`)
                                - **DDL 변환** 및 **데이터 적재** (동기 단건 / 비동기 배치)
                                - **카테고리** 및 **Extract Worker** 오케스트레이션

                                ## 공통 규칙
                                | 항목 | 설명 |
                                |------|------|
                                | Base URL | `/api/v1` |
                                | 인증 | PoC 기본값 **없음** (모든 API 공개) |
                                | `userId` 헤더 | CUD 작업 시 감사 컬럼(`creatr_id`, `mdfr_id`)에 기록. 생략 시 `system` |
                                | ID 형식 | `{prefix}-YYYYMMDD-NNN` (예: `lnkg-20260623-001`, `etl-20260623-002`) |
                                | 저장 방식 | 조회=MyBatis+뷰, CUD=**저장 프로시저** (`sp_*`, op=`C`/`U`/`D`) |

                                ## 지원 DB 벤더
                                `POSTGRESQL`, `MYSQL`, `MARIADB`, `ORACLE`, `CLICKHOUSE`

                                ## 마이그레이션 작업 상태 (`job_stts_cd`)
                                `PENDING` → `RUNNING` → `SUCCESS` / `PARTIAL_SUCCESS` / `FAILED` / `CANCELLED`

                                ## Swagger UI
                                - UI: `/swagger-ui.html`
                                - OpenAPI JSON: `/v3/api-docs`
                                """)
                        .contact(new Contact()
                                .name("CDW Catalog Control")
                                .email("cdw-catalog@example.com"))
                        .license(new License().name("Internal PoC")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("로컬 개발 서버")
                ))
                .tags(List.of(
                        tag("01-DB 연결", "원천·타깃 JDBC 연결 프로필 CRUD. 등록 시 헬스체크 수행."),
                        tag("02-메타데이터 조회", "메타데이터세트(`mtdtId`) 기준 테이블·코드유형 조회 (뷰 기반)."),
                        tag("03-메타데이터 동기화", "원천 DB 스키마 introspection 후 `t_mtdt_set`/`t_mtdt_tbl` 반영."),
                        tag("04-테이블 카테고리", "메타 테이블 카테고리 트리 및 테이블 매핑 관리."),
                        tag("05-DB 마이그레이션", "DDL 미리보기, 단건 동기 적재, 배치 비동기 적재, 작업 운영 API."),
                        tag("06-추출(Extract)", "Extract Worker HTTP 연동 (`cdw.catalog.extract-worker.enabled`).")
                ));
    }

    private static Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }
}
