create table t_etl_seq (
    seq_key varchar(32) not null,
    seq_dt  date not null,
    last_no integer not null default 0,
    primary key (seq_key, seq_dt)
);

create or replace view v_mtdt_tbl_list as
select
    t.mtdt_tbl_id, t.mtdt_id, t.orgnl_tbl_nm, t.tbl_nm, t.tbl_expln, t.orgnl_tbl_expln,
    t.sort_no, t.expsr_yn, t.src_exst_yn, t.use_yn, t.cd_tbl_yn, t.tbl_type_cd,
    s.whol_nocs, s.chg_nocs, s.stats_stts_cd, s.elps_ms_cnt,
    t.creatr_id, t.mdfr_id, t.crt_dt, t.mdfcn_dt
from t_mtdt_tbl t
left join t_mtdt_tbl_stats s on s.mtdt_tbl_id = t.mtdt_tbl_id
where t.use_yn = true
  and ((t.tbl_type_cd = 'SOURCE' and t.src_exst_yn = true) or t.tbl_type_cd = 'USER_DEFINED_JOIN');

create or replace view v_cd_type_list as
select
    t.mtdt_tbl_id, t.orgnl_tbl_nm, t.tbl_nm, t.src_exst_yn, t.cd_tbl_yn,
    true as reg_yn, c.cd_type_id, c.cd_type_nm, c.cd_type_expln, c.cd_col_nm, c.cd_nm_col_nm,
    coalesce(count(mc.mtdt_col_id), 0) as assigned_column_count,
    c.mdfr_id, c.mdfcn_dt, c.mtdt_id, t.sort_no
from t_cd_type c
inner join t_mtdt_tbl t on c.mtdt_id = t.mtdt_id and c.mtdt_tbl_id = t.mtdt_tbl_id
left join t_mtdt_col mc on mc.cd_type_id = c.cd_type_id
where t.use_yn = true and t.cd_tbl_yn = true
group by t.mtdt_tbl_id, t.orgnl_tbl_nm, t.tbl_nm, t.src_exst_yn, t.cd_tbl_yn, t.sort_no,
    c.cd_type_id, c.cd_type_nm, c.cd_type_expln, c.cd_col_nm, c.cd_nm_col_nm, c.mdfr_id, c.mdfcn_dt, c.mtdt_id;

create or replace view v_cd_type_candidate as
select
    t.mtdt_tbl_id, t.orgnl_tbl_nm, t.tbl_nm, t.src_exst_yn, t.cd_tbl_yn,
    false as reg_yn, cast(null as varchar) as cd_type_id, cast(null as varchar) as cd_type_nm,
    cast(null as varchar) as cd_type_expln, cast(null as varchar) as cd_col_nm,
    cast(null as varchar) as cd_nm_col_nm, 0 as assigned_column_count,
    cast(null as varchar) as mdfr_id, cast(null as timestamp) as mdfcn_dt, t.mtdt_id, t.sort_no
from t_mtdt_tbl t
left join t_cd_type c on c.mtdt_id = t.mtdt_id and c.mtdt_tbl_id = t.mtdt_tbl_id
where t.use_yn = true and t.cd_tbl_yn = true and c.cd_type_id is null;

create or replace view v_mtdt_tbl_ctgr_mpng as
select m.mtdt_tbl_ctgr_mpng_id, m.mtdt_tbl_ctgr_id, m.mtdt_tbl_id,
       t.orgnl_tbl_nm, t.tbl_nm, t.tbl_expln, m.sort_no, m.expsr_yn, t.src_exst_yn, t.tbl_type_cd
from t_mtdt_tbl_ctgr_mpng m
join t_mtdt_tbl t on t.mtdt_tbl_id = m.mtdt_tbl_id
where t.use_yn = true;

create or replace view v_lnkg_profile as
select lnkg_id, lnkg_nm, db_type_cd, srvr_addr, srvr_port_no, db_nm, schm_nm, lnkg_expln,
       acnt_id, enpswd, use_yn, lnkg_stts_cd, test_rslt_cd, last_test_dt,
       creatr_id, mdfr_id, crt_dt, mdfcn_dt
from t_lnkg_profile where lnkg_stts_cd <> 'DELETED';

create or replace view v_mig_job as
select j.*, s.lnkg_nm as src_lnkg_nm, t.lnkg_nm as trgt_lnkg_nm
from t_mig_job j
left join t_lnkg_profile s on s.lnkg_id = j.src_lnkg_id
left join t_lnkg_profile t on t.lnkg_id = j.trgt_lnkg_id;

create or replace view v_mig_job_tbl as
select * from t_mig_job_tbl;

comment on table t_etl_seq is '일자·접두어별 업무 식별자 발급 상태';
comment on column t_etl_seq.seq_key is '채번 접두어 (lnkg, etl, migt 등)';
comment on column t_etl_seq.seq_dt is '채번 기준 일자';
comment on column t_etl_seq.last_no is '당일 마지막 발급 순번';

comment on view v_mtdt_tbl_list is '메타데이터 테이블 목록. 통계 LEFT JOIN, 사용 중인 SOURCE·사용자정의 조인 테이블만 포함';
comment on view v_cd_type_list is '코드 유형이 등록된 코드 테이블 목록. 연결된 컬럼 수 포함';
comment on view v_cd_type_candidate is '코드 테이블이지만 코드 유형 미등록 테이블 목록';
comment on view v_mtdt_tbl_ctgr_mpng is '카테고리별 테이블 매핑 조회. 테이블 명·설명 조인';
comment on view v_lnkg_profile is '삭제(DELETED) 제외 활성 연결 프로필';
comment on view v_mig_job is '마이그레이션 작업 목록. 원천·타깃 연결명 조인';
comment on view v_mig_job_tbl is '마이그레이션 작업의 테이블 단위 실행 결과';
