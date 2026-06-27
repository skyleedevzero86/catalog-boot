create table t_lnkg_profile (
    lnkg_id varchar(36) primary key,
    lnkg_nm varchar(120) not null unique,
    db_type_cd varchar(32) not null,
    srvr_addr varchar(255) not null,
    srvr_port_no integer not null,
    db_nm varchar(255) not null,
    schm_nm varchar(255),
    lnkg_expln varchar(1000),
    acnt_id varchar(255) not null,
    enpswd text not null,
    use_yn boolean not null,
    lnkg_stts_cd varchar(32) not null,
    test_rslt_cd varchar(32) not null,
    last_test_dt timestamp,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null
);

create table t_etl_seq (
    seq_key varchar(32) not null,
    seq_dt date not null,
    last_no integer not null default 0,
    primary key (seq_key, seq_dt)
);

create table t_mtdt_set (
    mtdt_id varchar(36) primary key,
    lnkg_id varchar(36) not null,
    mtdt_nm varchar(120) not null unique,
    mtdt_expln varchar(1000),
    last_sync_dt timestamp,
    last_sync_stts_cd varchar(32) not null,
    last_sync_msg_cn varchar(1000),
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    foreign key (lnkg_id) references t_lnkg_profile (lnkg_id)
);

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
    foreign key (src_lnkg_id) references t_lnkg_profile (lnkg_id),
    foreign key (trgt_lnkg_id) references t_lnkg_profile (lnkg_id),
    foreign key (mtdt_id) references t_mtdt_set (mtdt_id)
);

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
    foreign key (mig_job_id) references t_mig_job (mig_job_id) on delete cascade
);

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

create alias if not exists fn_next_etl_id for "cdw.catalog.test.support.H2EtlIdGenerator.nextId";
