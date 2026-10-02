/*
 * ============================================================================
 * V4__h2_test_extract_procedures.sql (H2 통합테스트)
 * ----------------------------------------------------------------------------
 * 목적   : 추출 CUD 프로시저 Java Alias 스텁
 * 대상   : H2 / test Flyway
 * 의존   : V3__h2_test_extract_schema, H2StoredProcedures
 * ============================================================================
 */

create alias if not exists sp_extr_dmnd for "cdw.catalog.test.support.H2StoredProcedures.spExtrDmnd";
create alias if not exists sp_extr_datst for "cdw.catalog.test.support.H2StoredProcedures.spExtrDatst";
create alias if not exists sp_extr_excn for "cdw.catalog.test.support.H2StoredProcedures.spExtrExcn";
