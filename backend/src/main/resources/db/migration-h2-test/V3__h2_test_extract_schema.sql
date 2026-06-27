create table t_extr_dmnd (
    extr_dmnd_id varchar(36) primary key,
    dmnd_src_cd varchar(32) not null,
    extr_form_id varchar(36),
    extr_dmnd_nm varchar(200) not null,
    extr_dmnd_stts_cd varchar(32) not null,
    otsd_dmnd_id varchar(255),
    datst_cnt integer not null,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null
);

create table t_extr_datst (
    extr_datst_id varchar(36) primary key,
    extr_dmnd_id varchar(36) not null,
    extr_spcf_id varchar(36),
    datst_nm varchar(255) not null,
    extr_datst_stts_cd varchar(32) not null,
    sort_no integer not null,
    mnfst_cn text,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    foreign key (extr_dmnd_id) references t_extr_dmnd (extr_dmnd_id) on delete cascade
);

create table t_extr_excn (
    extr_excn_id varchar(36) primary key,
    extr_datst_id varchar(36) not null,
    excn_sn integer not null,
    excn_type_cd varchar(32) not null,
    excn_stts_cd varchar(32) not null,
    extr_job_id varchar(100),
    excn_bgng_dt timestamp,
    excn_end_dt timestamp,
    rslt_nocs bigint,
    rslt_strg_type_cd varchar(32),
    rslt_tbl_nm varchar(255),
    rslt_file_path text,
    fail_cn varchar(2000),
    checkpoint_state varchar(64),
    resumed_from_checkpoint boolean not null default false,
    resume_message varchar(1000),
    excn_sql_cn text,
    excn_sql_vrbl_cn text,
    excn_req_cn text,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    foreign key (extr_datst_id) references t_extr_datst (extr_datst_id) on delete cascade,
    unique (extr_datst_id, excn_sn)
);

create or replace view v_extr_datst as
select * from t_extr_datst;

create or replace view v_extr_excn as
select * from t_extr_excn;
