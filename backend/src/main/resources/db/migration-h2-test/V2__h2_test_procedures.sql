/*
 * ============================================================================
 * V2__h2_test_procedures.sql (H2 통합테스트)
 * ----------------------------------------------------------------------------
 * 목적   : 연결·마이그레이션 CUD 프로시저를 Java Alias 로 스텁
 * 대상   : H2 / test Flyway
 * 계약   : 메서드 시그니처는 PostgreSQL sp_* 호출부와 동일한 인자 순서를 유지
 * 의존   : cdw.catalog.test.support.H2StoredProcedures
 * ============================================================================
 */

-- 운영 plpgsql 대신 Java 스텁. 비즈니스 검증은 테스트 코드에서 수행.
create alias if not exists sp_lnkg_profile for "cdw.catalog.test.support.H2StoredProcedures.spLnkgProfile";
create alias if not exists sp_mig_job for "cdw.catalog.test.support.H2StoredProcedures.spMigJob";
create alias if not exists sp_mig_job_tbl for "cdw.catalog.test.support.H2StoredProcedures.spMigJobTbl";
create alias if not exists sp_mig_job_tbl_reset_failed for "cdw.catalog.test.support.H2StoredProcedures.spMigJobTblResetFailed";
