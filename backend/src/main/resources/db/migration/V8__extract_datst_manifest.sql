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
