/*
 * ============================================================================
 * V4__h2_fn_alias.sql (H2 테스트 전용)
 * ----------------------------------------------------------------------------
 * 목적   : H2 에서 PostgreSQL `fn_next_etl_id` 호출 계약을 Java Alias 로 대체
 * 대상   : H2 (테스트 프로필). 운영 Flyway locations 에 포함하지 않음.
 * 의존   : cdw.catalog.test.support.H2EtlIdGenerator
 * 주의   : 운영 plpgsql 동시성 보장과 동일하지 않다. 단위/통합테스트 전용.
 * ============================================================================
 */

-- H2 CREATE ALIAS → Java static nextId(String prefix)
create alias if not exists fn_next_etl_id for "cdw.catalog.test.support.H2EtlIdGenerator.nextId";
