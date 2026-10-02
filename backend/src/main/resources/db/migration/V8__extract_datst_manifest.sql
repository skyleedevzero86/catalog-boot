/*
 * ============================================================================
 * V8__extract_datst_manifest.sql
 * ----------------------------------------------------------------------------
 * 목적   : 추출 데이터셋 매니페스트 JSON 저장 컬럼 및 조회 VIEW
 * 대상   : PostgreSQL / etl_data
 * 의존   : V1 (t_extr_datst, t_extr_excn)
 * 설계   : PoC 파이프라인에서 스테이징/컬럼/파일경로를 mnfst_cn(JSON)으로 영속화.
 *          extr_spcf_id 는 스펙 없이 prepare 하는 경로를 위해 NULL 허용.
 * 롤백   : ALTER ... DROP COLUMN mnfst_cn; VIEW 재정의
 * ============================================================================
 */

-- ---------------------------------------------------------------------------
-- t_extr_datst : 스펙 없이 prepare 가능하도록 extr_spcf_id NULL 허용
-- mnfst_cn : Worker 재시작 후에도 파이프라인 상태를 복원하기 위한 JSON
-- VIEW 는 물리 테이블 1:1 노출 (향후 조인 확장 지점)
-- ---------------------------------------------------------------------------
alter table t_extr_datst
    alter column extr_spcf_id drop not null;

alter table t_extr_datst
    add column if not exists mnfst_cn text;

comment on column t_extr_datst.mnfst_cn is '추출 데이터셋 매니페스트 JSON (PoC 파이프라인)';

create or replace view v_extr_datst as
select *
from t_extr_datst;

create or replace view v_extr_excn as
select *
from t_extr_excn;
