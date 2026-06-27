create table t_mig_job (
    mig_job_id varchar(36) primary key,
    src_lnkg_id varchar(36) not null,
    trgt_lnkg_id varchar(36) not null,
    mtdt_id varchar(36),
    src_schm_nm varchar(255),
    trgt_schm_nm varchar(255) not null,
    batch_sz integer not null default 500,
    drop_exst_yn boolean not null default true,
    job_stts_cd varchar(32) not null,
    tot_tbl_cnt integer not null default 0,
    succ_tbl_cnt integer not null default 0,
    fail_tbl_cnt integer not null default 0,
    tot_row_cnt bigint not null default 0,
    err_msg_cn varchar(4000),
    bgng_dt timestamp,
    end_dt timestamp,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_mig_job_src_lnkg
        foreign key (src_lnkg_id) references t_lnkg_profile (lnkg_id),
    constraint fk_t_mig_job_trgt_lnkg
        foreign key (trgt_lnkg_id) references t_lnkg_profile (lnkg_id),
    constraint fk_t_mig_job_mtdt_set
        foreign key (mtdt_id) references t_mtdt_set (mtdt_id),
    constraint ck_t_mig_job_stts
        check (job_stts_cd in ('PENDING', 'RUNNING', 'SUCCESS', 'PARTIAL_SUCCESS', 'FAILED'))
);

create index idx_t_mig_job_stts on t_mig_job (job_stts_cd);
create index idx_t_mig_job_crt_dt on t_mig_job (crt_dt desc);

create table t_mig_job_tbl (
    mig_job_tbl_id varchar(36) primary key,
    mig_job_id varchar(36) not null,
    tbl_nm varchar(255) not null,
    tbl_stts_cd varchar(32) not null,
    row_cnt bigint,
    batch_cnt integer,
    crt_tbl_ddl_cn text,
    err_msg_cn varchar(4000),
    bgng_dt timestamp,
    end_dt timestamp,
    sort_no integer not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_mig_job_tbl_job
        foreign key (mig_job_id) references t_mig_job (mig_job_id)
            on delete cascade,
    constraint ck_t_mig_job_tbl_stts
        check (tbl_stts_cd in ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED'))
);

create index idx_t_mig_job_tbl_job on t_mig_job_tbl (mig_job_id, sort_no);
comment on table t_mig_job is '원천-타깃 간 다중 테이블 적재 배치 작업 헤더';
comment on column t_mig_job.mig_job_id is '작업 식별자 (etl-YYYYMMDD-NNN)';
comment on column t_mig_job.src_lnkg_id is '원천 DB 연결 프로필 ID';
comment on column t_mig_job.trgt_lnkg_id is '타깃 DB 연결 프로필 ID';
comment on column t_mig_job.mtdt_id is '적재 대상 테이블 목록을 가져올 메타데이터 세트 ID (선택)';
comment on column t_mig_job.src_schm_nm is '원천 스키마명';
comment on column t_mig_job.trgt_schm_nm is '타깃 스키마명';
comment on column t_mig_job.batch_sz is 'INSERT 배치 크기';
comment on column t_mig_job.drop_exst_yn is '적재 전 타깃 테이블 DROP 여부';
comment on column t_mig_job.job_stts_cd is '작업 상태 (PENDING/RUNNING/SUCCESS/PARTIAL_SUCCESS/FAILED)';
comment on column t_mig_job.tot_tbl_cnt is '대상 테이블 수';
comment on column t_mig_job.succ_tbl_cnt is '성공 테이블 수';
comment on column t_mig_job.fail_tbl_cnt is '실패 테이블 수';
comment on column t_mig_job.tot_row_cnt is '전체 적재 행 수 합계';
comment on column t_mig_job.err_msg_cn is '작업 수준 오류 메시지';
comment on column t_mig_job.bgng_dt is '작업 시작 시각';
comment on column t_mig_job.end_dt is '작업 종료 시각';

comment on table t_mig_job_tbl is '배치 작업 내 테이블 단위 적재 실행 이력';
comment on column t_mig_job_tbl.mig_job_tbl_id is '테이블 작업 식별자 (migt-YYYYMMDD-NNN)';
comment on column t_mig_job_tbl.mig_job_id is '상위 배치 작업 ID';
comment on column t_mig_job_tbl.tbl_nm is '적재 대상 테이블명 (원천 기준)';
comment on column t_mig_job_tbl.tbl_stts_cd is '테이블 작업 상태 (PENDING/RUNNING/SUCCESS/FAILED)';
comment on column t_mig_job_tbl.row_cnt is '적재된 행 수';
comment on column t_mig_job_tbl.batch_cnt is '실행된 INSERT 배치 횟수';
comment on column t_mig_job_tbl.crt_tbl_ddl_cn is '타깃에 생성된 CREATE TABLE DDL';
comment on column t_mig_job_tbl.err_msg_cn is '테이블 단위 오류 메시지';
comment on column t_mig_job_tbl.bgng_dt is '테이블 적재 시작 시각';
comment on column t_mig_job_tbl.end_dt is '테이블 적재 종료 시각';
comment on column t_mig_job_tbl.sort_no is '배치 내 테이블 실행 순서';
