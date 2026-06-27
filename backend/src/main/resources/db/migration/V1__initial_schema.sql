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
    mdfcn_dt timestamp not null,
    constraint ck_t_lnkg_profile_db_type
        check (db_type_cd in ('POSTGRESQL', 'MYSQL', 'MARIADB', 'ORACLE', 'CLICKHOUSE')),
    constraint ck_t_lnkg_profile_status
        check (lnkg_stts_cd in ('DRAFT', 'ACTIVE', 'DISABLED', 'DELETED')),
    constraint ck_t_lnkg_profile_test_status
        check (test_rslt_cd in ('UNKNOWN', 'VALIDATING', 'HEALTHY', 'UNHEALTHY'))
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
    constraint fk_t_mtdt_set_lnkg_profile
        foreign key (lnkg_id) references t_lnkg_profile (lnkg_id),
    constraint ck_t_mtdt_set_sync_status
        check (last_sync_stts_cd in ('NEVER', 'RUNNING', 'SUCCESS', 'FAILED'))
);

create table t_mtdt_tbl (
    mtdt_tbl_id varchar(36) primary key,
    mtdt_id varchar(36) not null,
    orgnl_tbl_nm varchar(255) not null,
    tbl_nm varchar(255) not null,
    tbl_expln varchar(1000),
    orgnl_tbl_expln varchar(1000),
    sort_no integer not null,
    expsr_yn boolean not null,
    src_exst_yn boolean not null,
    use_yn boolean not null default true,
    cd_tbl_yn boolean not null default false,
    tbl_type_cd varchar(32) not null default 'SOURCE',
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint uq_t_mtdt_tbl_source unique (mtdt_id, orgnl_tbl_nm),
    constraint fk_t_mtdt_tbl_mtdt_set
        foreign key (mtdt_id) references t_mtdt_set (mtdt_id)
            on delete cascade,
    constraint ck_t_mtdt_tbl_type
        check (tbl_type_cd in ('SOURCE', 'USER_DEFINED_JOIN'))
);

create table t_mtdt_tbl_stats (
    mtdt_tbl_id varchar(36) primary key,
    whol_nocs bigint not null default 0,
    chg_nocs bigint,
    stats_stts_cd varchar(32) not null,
    elps_ms_cnt bigint,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_mtdt_tbl_stats_mtdt_tbl
        foreign key (mtdt_tbl_id) references t_mtdt_tbl (mtdt_tbl_id)
            on delete cascade,
    constraint ck_t_mtdt_tbl_stats_status
        check (stats_stts_cd in ('NEVER', 'RUNNING', 'SUCCESS', 'FAILED'))
);

create table t_mtdt_tbl_ctgr (
    mtdt_tbl_ctgr_id varchar(36) primary key,
    mtdt_id varchar(36) not null,
    up_mtdt_tbl_ctgr_id varchar(36),
    ctgr_nm varchar(120) not null,
    ctgr_expln varchar(1000),
    sort_no integer not null,
    expsr_yn boolean not null,
    lwr_ctgr_prm_yn boolean not null,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_mtdt_tbl_ctgr_mtdt_set
        foreign key (mtdt_id) references t_mtdt_set (mtdt_id)
            on delete cascade,
    constraint fk_t_mtdt_tbl_ctgr_parent
        foreign key (up_mtdt_tbl_ctgr_id) references t_mtdt_tbl_ctgr (mtdt_tbl_ctgr_id)
            on delete cascade,
    constraint uq_t_mtdt_tbl_ctgr_parent_name
        unique (mtdt_id, up_mtdt_tbl_ctgr_id, ctgr_nm)
);

create table t_mtdt_tbl_ctgr_mpng (
    mtdt_tbl_ctgr_mpng_id varchar(36) primary key,
    mtdt_id varchar(36) not null,
    mtdt_tbl_ctgr_id varchar(36) not null,
    mtdt_tbl_id varchar(36) not null,
    sort_no integer not null,
    expsr_yn boolean not null,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_mtdt_tbl_ctgr_mpng_mtdt_set
        foreign key (mtdt_id) references t_mtdt_set (mtdt_id)
            on delete cascade,
    constraint fk_t_mtdt_tbl_ctgr_mpng_ctgr
        foreign key (mtdt_tbl_ctgr_id) references t_mtdt_tbl_ctgr (mtdt_tbl_ctgr_id)
            on delete cascade,
    constraint fk_t_mtdt_tbl_ctgr_mpng_mtdt_tbl
        foreign key (mtdt_tbl_id) references t_mtdt_tbl (mtdt_tbl_id)
            on delete cascade,
    constraint uq_t_mtdt_tbl_ctgr_mpng_ctgr_tbl
        unique (mtdt_tbl_ctgr_id, mtdt_tbl_id)
);

create table t_cd_type (
    cd_type_id varchar(36) primary key,
    mtdt_id varchar(36) not null,
    mtdt_tbl_id varchar(36) not null,
    cd_type_nm varchar(120) not null,
    cd_type_expln varchar(1000),
    cd_tbl_nm varchar(255) not null,
    cd_col_nm varchar(255) not null,
    cd_nm_col_nm varchar(255) not null,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint uq_t_cd_type_meta_table unique (mtdt_id, mtdt_tbl_id),
    constraint fk_t_cd_type_mtdt_set
        foreign key (mtdt_id) references t_mtdt_set (mtdt_id)
            on delete cascade,
    constraint fk_t_cd_type_mtdt_tbl
        foreign key (mtdt_tbl_id) references t_mtdt_tbl (mtdt_tbl_id)
            on delete cascade
);

create table t_mtdt_col (
    mtdt_col_id varchar(36) primary key,
    mtdt_id varchar(36) not null,
    mtdt_tbl_id varchar(36) not null,
    orgnl_col_nm varchar(255) not null,
    col_nm varchar(255) not null,
    col_expln varchar(1000),
    orgnl_col_expln varchar(1000),
    db_type_nm varchar(255),
    col_len integer,
    dcpt_dgt integer,
    nul_prm_yn boolean not null,
    pk_yn boolean not null,
    sort_no integer not null,
    expsr_yn boolean not null,
    prvc_level_cd varchar(32) not null,
    cd_type_id varchar(36),
    bsc_view_yn boolean not null,
    fltr_type_cd varchar(32) not null,
    src_exst_yn boolean not null,
    col_type_cd varchar(32) not null default 'SOURCE',
    src_user_dfn_tbl_cpst_id varchar(36),
    src_mtdt_col_id varchar(36),
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_mtdt_col_mtdt_set
        foreign key (mtdt_id) references t_mtdt_set (mtdt_id)
            on delete cascade,
    constraint fk_t_mtdt_col_mtdt_tbl
        foreign key (mtdt_tbl_id) references t_mtdt_tbl (mtdt_tbl_id)
            on delete cascade,
    constraint fk_t_mtdt_col_cd_type
        foreign key (cd_type_id) references t_cd_type (cd_type_id)
            on delete set null,
    constraint fk_t_mtdt_col_src_col
        foreign key (src_mtdt_col_id) references t_mtdt_col (mtdt_col_id)
            on delete set null,
    constraint ck_t_mtdt_col_privacy
        check (prvc_level_cd in ('NONE', 'PERSONAL', 'SENSITIVE')),
    constraint ck_t_mtdt_col_filter_type
        check (fltr_type_cd in ('TEXT', 'NUMBER', 'DATE', 'DATETIME', 'CODE', 'BOOLEAN')),
    constraint ck_t_mtdt_col_type
        check (col_type_cd in ('SOURCE', 'USER_DEFINED_JOIN_COPY'))
);

create table t_mtdt_user_dfn_tbl_cpst (
    user_dfn_tbl_cpst_id varchar(36) primary key,
    mtdt_id varchar(36) not null,
    user_dfn_mtdt_tbl_id varchar(36) not null,
    src_mtdt_tbl_id varchar(36) not null,
    tbl_ncnm varchar(64) not null,
    up_cpst_id varchar(36),
    jn_type_cd varchar(32),
    up_jn_mtdt_col_id varchar(36),
    src_jn_mtdt_col_id varchar(36),
    jn_ref_stts_cd varchar(32) not null default 'VALID',
    sort_no integer not null,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_mtdt_user_dfn_tbl_cpst_mtdt_set
        foreign key (mtdt_id) references t_mtdt_set (mtdt_id)
            on delete cascade,
    constraint fk_t_mtdt_user_dfn_tbl_cpst_user_dfn_tbl
        foreign key (user_dfn_mtdt_tbl_id) references t_mtdt_tbl (mtdt_tbl_id)
            on delete cascade,
    constraint fk_t_mtdt_user_dfn_tbl_cpst_src_tbl
        foreign key (src_mtdt_tbl_id) references t_mtdt_tbl (mtdt_tbl_id)
            on delete cascade,
    constraint fk_t_mtdt_user_dfn_tbl_cpst_up_cpst
        foreign key (up_cpst_id) references t_mtdt_user_dfn_tbl_cpst (user_dfn_tbl_cpst_id)
            on delete cascade,
    constraint fk_t_mtdt_user_dfn_tbl_cpst_up_join_col
        foreign key (up_jn_mtdt_col_id) references t_mtdt_col (mtdt_col_id)
            on delete set null,
    constraint fk_t_mtdt_user_dfn_tbl_cpst_src_join_col
        foreign key (src_jn_mtdt_col_id) references t_mtdt_col (mtdt_col_id)
            on delete set null,
    constraint ck_t_mtdt_user_dfn_tbl_cpst_join_type
        check (jn_type_cd is null or jn_type_cd in ('INNER', 'LEFT')),
    constraint ck_t_mtdt_user_dfn_tbl_cpst_join_ref_status
        check (jn_ref_stts_cd in ('VALID', 'SOURCE_TABLE_MISSING', 'SOURCE_COLUMN_MISSING')),
    constraint ck_t_mtdt_user_dfn_tbl_cpst_join_required
        check (
            (up_cpst_id is null and jn_type_cd is null and up_jn_mtdt_col_id is null and src_jn_mtdt_col_id is null)
            or
            (up_cpst_id is not null and jn_type_cd is not null and up_jn_mtdt_col_id is not null and src_jn_mtdt_col_id is not null)
        )
);

create table t_mtdt_user_dfn_tbl_jn_cond (
    user_dfn_tbl_jn_cond_id varchar(36) primary key,
    mtdt_id varchar(36) not null,
    user_dfn_tbl_cpst_id varchar(36) not null,
    up_jn_mtdt_col_id varchar(36) not null,
    src_jn_mtdt_col_id varchar(36) not null,
    sort_no integer not null,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_mtdt_user_dfn_tbl_jn_cond_mtdt_set
        foreign key (mtdt_id) references t_mtdt_set (mtdt_id)
            on delete cascade,
    constraint fk_t_mtdt_user_dfn_tbl_jn_cond_cpst
        foreign key (user_dfn_tbl_cpst_id) references t_mtdt_user_dfn_tbl_cpst (user_dfn_tbl_cpst_id)
            on delete cascade,
    constraint fk_t_mtdt_user_dfn_tbl_jn_cond_up_col
        foreign key (up_jn_mtdt_col_id) references t_mtdt_col (mtdt_col_id)
            on delete cascade,
    constraint fk_t_mtdt_user_dfn_tbl_jn_cond_src_col
        foreign key (src_jn_mtdt_col_id) references t_mtdt_col (mtdt_col_id)
            on delete cascade
);

alter table t_mtdt_col
    add constraint fk_t_mtdt_col_src_user_dfn_cpst
        foreign key (src_user_dfn_tbl_cpst_id) references t_mtdt_user_dfn_tbl_cpst (user_dfn_tbl_cpst_id)
            on delete set null;

create index idx_t_mtdt_set_lnkg_id
    on t_mtdt_set (lnkg_id);

create index idx_t_mtdt_tbl_src_exst_yn
    on t_mtdt_tbl (mtdt_id, src_exst_yn);

create index idx_t_mtdt_tbl_use_yn
    on t_mtdt_tbl (mtdt_id, use_yn);

create index idx_t_mtdt_tbl_cd_tbl_yn
    on t_mtdt_tbl (mtdt_id, cd_tbl_yn);

create index idx_t_mtdt_tbl_type
    on t_mtdt_tbl (mtdt_id, tbl_type_cd);

create index idx_t_mtdt_tbl_ctgr_mtdt_id
    on t_mtdt_tbl_ctgr (mtdt_id);

create index idx_t_mtdt_tbl_ctgr_parent_id
    on t_mtdt_tbl_ctgr (up_mtdt_tbl_ctgr_id);

create index idx_t_mtdt_tbl_ctgr_mpng_ctgr_id
    on t_mtdt_tbl_ctgr_mpng (mtdt_tbl_ctgr_id);

create index idx_t_mtdt_tbl_ctgr_mpng_tbl_id
    on t_mtdt_tbl_ctgr_mpng (mtdt_tbl_id);

create index idx_t_cd_type_mtdt_id
    on t_cd_type (mtdt_id);

create index idx_t_cd_type_mtdt_tbl_id
    on t_cd_type (mtdt_tbl_id);

create index idx_t_mtdt_col_mtdt_id
    on t_mtdt_col (mtdt_id);

create index idx_t_mtdt_col_src_exst_yn
    on t_mtdt_col (mtdt_tbl_id, src_exst_yn);

create index idx_t_mtdt_col_exposed_src_exst_yn
    on t_mtdt_col (mtdt_tbl_id, src_exst_yn, expsr_yn);

create index idx_t_mtdt_col_cd_type_id
    on t_mtdt_col (cd_type_id);

create index idx_t_mtdt_col_type
    on t_mtdt_col (mtdt_tbl_id, col_type_cd);

create index idx_t_mtdt_col_src_ref
    on t_mtdt_col (src_mtdt_col_id);

create unique index uq_t_mtdt_col_source
    on t_mtdt_col (mtdt_tbl_id, orgnl_col_nm)
    where col_type_cd = 'SOURCE';

create unique index uq_t_mtdt_col_user_defined_source
    on t_mtdt_col (mtdt_tbl_id, src_user_dfn_tbl_cpst_id, src_mtdt_col_id)
    where col_type_cd = 'USER_DEFINED_JOIN_COPY';

create index idx_t_mtdt_user_dfn_tbl_cpst_user_dfn_tbl
    on t_mtdt_user_dfn_tbl_cpst (user_dfn_mtdt_tbl_id);

create index idx_t_mtdt_user_dfn_tbl_cpst_src_tbl
    on t_mtdt_user_dfn_tbl_cpst (src_mtdt_tbl_id);

create unique index uq_t_mtdt_user_dfn_tbl_cpst_tbl_ncnm
    on t_mtdt_user_dfn_tbl_cpst (user_dfn_mtdt_tbl_id, tbl_ncnm);

create index idx_t_mtdt_user_dfn_tbl_cpst_up_join_col
    on t_mtdt_user_dfn_tbl_cpst (up_jn_mtdt_col_id);

create index idx_t_mtdt_user_dfn_tbl_cpst_src_join_col
    on t_mtdt_user_dfn_tbl_cpst (src_jn_mtdt_col_id);

create index idx_t_mtdt_user_dfn_tbl_jn_cond_cpst
    on t_mtdt_user_dfn_tbl_jn_cond (user_dfn_tbl_cpst_id, sort_no);

create index idx_t_mtdt_user_dfn_tbl_jn_cond_up_col
    on t_mtdt_user_dfn_tbl_jn_cond (up_jn_mtdt_col_id);

create index idx_t_mtdt_user_dfn_tbl_jn_cond_src_col
    on t_mtdt_user_dfn_tbl_jn_cond (src_jn_mtdt_col_id);

comment on table t_lnkg_profile is '연결프로필';
comment on column t_lnkg_profile.lnkg_id is '연결아이디';
comment on column t_lnkg_profile.lnkg_nm is '연결명';
comment on column t_lnkg_profile.db_type_cd is '데이터베이스유형코드';
comment on column t_lnkg_profile.srvr_addr is '서버주소';
comment on column t_lnkg_profile.srvr_port_no is '서버포트번호';
comment on column t_lnkg_profile.db_nm is '데이터베이스명';
comment on column t_lnkg_profile.schm_nm is '스키마명';
comment on column t_lnkg_profile.lnkg_expln is '연결설명';
comment on column t_lnkg_profile.acnt_id is '계정아이디';
comment on column t_lnkg_profile.enpswd is '암호화비밀번호';
comment on column t_lnkg_profile.use_yn is '사용여부';
comment on column t_lnkg_profile.lnkg_stts_cd is '연결상태코드';
comment on column t_lnkg_profile.test_rslt_cd is '시험결과코드';
comment on column t_lnkg_profile.last_test_dt is '최종시험일시';
comment on column t_lnkg_profile.creatr_id is '생성자아이디';
comment on column t_lnkg_profile.mdfr_id is '수정자아이디';
comment on column t_lnkg_profile.crt_dt is '생성일시';
comment on column t_lnkg_profile.mdfcn_dt is '수정일시';

comment on table t_mtdt_set is '메타데이터세트';
comment on column t_mtdt_set.mtdt_id is '메타데이터아이디';
comment on column t_mtdt_set.lnkg_id is '연결아이디';
comment on column t_mtdt_set.mtdt_nm is '메타데이터명';
comment on column t_mtdt_set.mtdt_expln is '메타데이터설명';
comment on column t_mtdt_set.last_sync_dt is '최종동기화일시';
comment on column t_mtdt_set.last_sync_stts_cd is '최종동기화상태코드';
comment on column t_mtdt_set.last_sync_msg_cn is '최종동기화메시지내용';
comment on column t_mtdt_set.creatr_id is '생성자아이디';
comment on column t_mtdt_set.mdfr_id is '수정자아이디';
comment on column t_mtdt_set.crt_dt is '생성일시';
comment on column t_mtdt_set.mdfcn_dt is '수정일시';

comment on table t_mtdt_tbl is '메타데이터테이블';
comment on column t_mtdt_tbl.mtdt_tbl_id is '메타데이터테이블아이디';
comment on column t_mtdt_tbl.mtdt_id is '메타데이터아이디';
comment on column t_mtdt_tbl.orgnl_tbl_nm is '원본테이블명';
comment on column t_mtdt_tbl.tbl_nm is '테이블명';
comment on column t_mtdt_tbl.tbl_expln is '테이블설명';
comment on column t_mtdt_tbl.orgnl_tbl_expln is '원본테이블설명';
comment on column t_mtdt_tbl.sort_no is '정렬번호';
comment on column t_mtdt_tbl.expsr_yn is '노출여부';
comment on column t_mtdt_tbl.src_exst_yn is '출처존재여부';
comment on column t_mtdt_tbl.use_yn is '사용여부';
comment on column t_mtdt_tbl.cd_tbl_yn is '코드테이블여부';
comment on column t_mtdt_tbl.tbl_type_cd is '테이블유형코드';
comment on column t_mtdt_tbl.creatr_id is '생성자아이디';
comment on column t_mtdt_tbl.mdfr_id is '수정자아이디';
comment on column t_mtdt_tbl.crt_dt is '생성일시';
comment on column t_mtdt_tbl.mdfcn_dt is '수정일시';

comment on table t_mtdt_tbl_stats is '메타데이터테이블통계';
comment on column t_mtdt_tbl_stats.mtdt_tbl_id is '메타데이터테이블아이디';
comment on column t_mtdt_tbl_stats.whol_nocs is '전체건수';
comment on column t_mtdt_tbl_stats.chg_nocs is '변경건수';
comment on column t_mtdt_tbl_stats.stats_stts_cd is '통계상태코드';
comment on column t_mtdt_tbl_stats.elps_ms_cnt is '경과밀리초수';
comment on column t_mtdt_tbl_stats.crt_dt is '생성일시';
comment on column t_mtdt_tbl_stats.mdfcn_dt is '수정일시';

comment on table t_mtdt_tbl_ctgr is '메타데이터테이블카테고리';
comment on column t_mtdt_tbl_ctgr.mtdt_tbl_ctgr_id is '메타데이터테이블카테고리아이디';
comment on column t_mtdt_tbl_ctgr.mtdt_id is '메타데이터아이디';
comment on column t_mtdt_tbl_ctgr.up_mtdt_tbl_ctgr_id is '상위메타데이터테이블카테고리아이디';
comment on column t_mtdt_tbl_ctgr.ctgr_nm is '카테고리명';
comment on column t_mtdt_tbl_ctgr.ctgr_expln is '카테고리설명';
comment on column t_mtdt_tbl_ctgr.sort_no is '정렬번호';
comment on column t_mtdt_tbl_ctgr.expsr_yn is '노출여부';
comment on column t_mtdt_tbl_ctgr.lwr_ctgr_prm_yn is '하위카테고리허용여부';
comment on column t_mtdt_tbl_ctgr.creatr_id is '생성자아이디';
comment on column t_mtdt_tbl_ctgr.mdfr_id is '수정자아이디';
comment on column t_mtdt_tbl_ctgr.crt_dt is '생성일시';
comment on column t_mtdt_tbl_ctgr.mdfcn_dt is '수정일시';

comment on table t_mtdt_tbl_ctgr_mpng is '메타데이터테이블카테고리매핑';
comment on column t_mtdt_tbl_ctgr_mpng.mtdt_tbl_ctgr_mpng_id is '메타데이터테이블카테고리매핑아이디';
comment on column t_mtdt_tbl_ctgr_mpng.mtdt_id is '메타데이터아이디';
comment on column t_mtdt_tbl_ctgr_mpng.mtdt_tbl_ctgr_id is '메타데이터테이블카테고리아이디';
comment on column t_mtdt_tbl_ctgr_mpng.mtdt_tbl_id is '메타데이터테이블아이디';
comment on column t_mtdt_tbl_ctgr_mpng.sort_no is '정렬번호';
comment on column t_mtdt_tbl_ctgr_mpng.expsr_yn is '노출여부';
comment on column t_mtdt_tbl_ctgr_mpng.creatr_id is '생성자아이디';
comment on column t_mtdt_tbl_ctgr_mpng.mdfr_id is '수정자아이디';
comment on column t_mtdt_tbl_ctgr_mpng.crt_dt is '생성일시';
comment on column t_mtdt_tbl_ctgr_mpng.mdfcn_dt is '수정일시';

comment on table t_cd_type is '코드유형';
comment on column t_cd_type.cd_type_id is '코드유형아이디';
comment on column t_cd_type.mtdt_id is '메타데이터아이디';
comment on column t_cd_type.mtdt_tbl_id is '메타데이터테이블아이디';
comment on column t_cd_type.cd_type_nm is '코드유형명';
comment on column t_cd_type.cd_type_expln is '코드유형설명';
comment on column t_cd_type.cd_tbl_nm is '코드테이블명';
comment on column t_cd_type.cd_col_nm is '코드컬럼명';
comment on column t_cd_type.cd_nm_col_nm is '코드명컬럼명';
comment on column t_cd_type.creatr_id is '생성자아이디';
comment on column t_cd_type.mdfr_id is '수정자아이디';
comment on column t_cd_type.crt_dt is '생성일시';
comment on column t_cd_type.mdfcn_dt is '수정일시';

comment on table t_mtdt_col is '메타데이터컬럼';
comment on column t_mtdt_col.mtdt_col_id is '메타데이터컬럼아이디';
comment on column t_mtdt_col.mtdt_id is '메타데이터아이디';
comment on column t_mtdt_col.mtdt_tbl_id is '메타데이터테이블아이디';
comment on column t_mtdt_col.orgnl_col_nm is '원본컬럼명';
comment on column t_mtdt_col.col_nm is '컬럼명';
comment on column t_mtdt_col.col_expln is '컬럼설명';
comment on column t_mtdt_col.orgnl_col_expln is '원본컬럼설명';
comment on column t_mtdt_col.db_type_nm is '데이터베이스유형명';
comment on column t_mtdt_col.col_len is '컬럼길이';
comment on column t_mtdt_col.dcpt_dgt is '소수점자릿수';
comment on column t_mtdt_col.nul_prm_yn is '널허용여부';
comment on column t_mtdt_col.pk_yn is '기본키여부';
comment on column t_mtdt_col.sort_no is '정렬번호';
comment on column t_mtdt_col.expsr_yn is '노출여부';
comment on column t_mtdt_col.prvc_level_cd is '개인정보수준코드';
comment on column t_mtdt_col.cd_type_id is '코드유형아이디';
comment on column t_mtdt_col.bsc_view_yn is '기본보기여부';
comment on column t_mtdt_col.fltr_type_cd is '필터유형코드';
comment on column t_mtdt_col.src_exst_yn is '출처존재여부';
comment on column t_mtdt_col.col_type_cd is '컬럼유형코드';
comment on column t_mtdt_col.src_user_dfn_tbl_cpst_id is '출처사용자정의테이블구성아이디';
comment on column t_mtdt_col.src_mtdt_col_id is '출처메타데이터컬럼아이디';
comment on column t_mtdt_col.creatr_id is '생성자아이디';
comment on column t_mtdt_col.mdfr_id is '수정자아이디';
comment on column t_mtdt_col.crt_dt is '생성일시';
comment on column t_mtdt_col.mdfcn_dt is '수정일시';

comment on table t_mtdt_user_dfn_tbl_cpst is '메타데이터사용자정의테이블구성';
comment on column t_mtdt_user_dfn_tbl_cpst.user_dfn_tbl_cpst_id is '사용자정의테이블구성아이디';
comment on column t_mtdt_user_dfn_tbl_cpst.mtdt_id is '메타데이터아이디';
comment on column t_mtdt_user_dfn_tbl_cpst.user_dfn_mtdt_tbl_id is '사용자정의메타데이터테이블아이디';
comment on column t_mtdt_user_dfn_tbl_cpst.src_mtdt_tbl_id is '출처메타데이터테이블아이디';
comment on column t_mtdt_user_dfn_tbl_cpst.tbl_ncnm is '테이블별칭';
comment on column t_mtdt_user_dfn_tbl_cpst.up_cpst_id is '상위구성아이디';
comment on column t_mtdt_user_dfn_tbl_cpst.jn_type_cd is '조인유형코드';
comment on column t_mtdt_user_dfn_tbl_cpst.up_jn_mtdt_col_id is '상위조인메타데이터컬럼아이디';
comment on column t_mtdt_user_dfn_tbl_cpst.src_jn_mtdt_col_id is '출처조인메타데이터컬럼아이디';
comment on column t_mtdt_user_dfn_tbl_cpst.jn_ref_stts_cd is '조인참조상태코드';
comment on column t_mtdt_user_dfn_tbl_cpst.sort_no is '정렬번호';
comment on column t_mtdt_user_dfn_tbl_cpst.creatr_id is '생성자아이디';
comment on column t_mtdt_user_dfn_tbl_cpst.mdfr_id is '수정자아이디';
comment on column t_mtdt_user_dfn_tbl_cpst.crt_dt is '생성일시';
comment on column t_mtdt_user_dfn_tbl_cpst.mdfcn_dt is '수정일시';

comment on table t_mtdt_user_dfn_tbl_jn_cond is '메타데이터사용자정의테이블조인조건';
comment on column t_mtdt_user_dfn_tbl_jn_cond.user_dfn_tbl_jn_cond_id is '사용자정의테이블조인조건아이디';
comment on column t_mtdt_user_dfn_tbl_jn_cond.mtdt_id is '메타데이터아이디';
comment on column t_mtdt_user_dfn_tbl_jn_cond.user_dfn_tbl_cpst_id is '사용자정의테이블구성아이디';
comment on column t_mtdt_user_dfn_tbl_jn_cond.up_jn_mtdt_col_id is '상위조인메타데이터컬럼아이디';
comment on column t_mtdt_user_dfn_tbl_jn_cond.src_jn_mtdt_col_id is '출처조인메타데이터컬럼아이디';
comment on column t_mtdt_user_dfn_tbl_jn_cond.sort_no is '정렬번호';
comment on column t_mtdt_user_dfn_tbl_jn_cond.creatr_id is '생성자아이디';
comment on column t_mtdt_user_dfn_tbl_jn_cond.mdfr_id is '수정자아이디';
comment on column t_mtdt_user_dfn_tbl_jn_cond.crt_dt is '생성일시';
comment on column t_mtdt_user_dfn_tbl_jn_cond.mdfcn_dt is '수정일시';

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'uq_t_mtdt_tbl_mtdt_tbl_id') then
        alter table t_mtdt_tbl
            add constraint uq_t_mtdt_tbl_mtdt_tbl_id unique (mtdt_id, mtdt_tbl_id);
    end if;

    if not exists (select 1 from pg_constraint where conname = 'uq_t_mtdt_tbl_ctgr_mtdt_ctgr_id') then
        alter table t_mtdt_tbl_ctgr
            add constraint uq_t_mtdt_tbl_ctgr_mtdt_ctgr_id unique (mtdt_id, mtdt_tbl_ctgr_id);
    end if;

    if not exists (select 1 from pg_constraint where conname = 'uq_t_cd_type_mtdt_cd_type_id') then
        alter table t_cd_type
            add constraint uq_t_cd_type_mtdt_cd_type_id unique (mtdt_id, cd_type_id);
    end if;

    if not exists (select 1 from pg_constraint where conname = 'uq_t_mtdt_col_mtdt_col_id') then
        alter table t_mtdt_col
            add constraint uq_t_mtdt_col_mtdt_col_id unique (mtdt_id, mtdt_col_id);
    end if;

    if not exists (select 1 from pg_constraint where conname = 'uq_t_mtdt_user_dfn_tbl_cpst_mtdt_cpst_id') then
        alter table t_mtdt_user_dfn_tbl_cpst
            add constraint uq_t_mtdt_user_dfn_tbl_cpst_mtdt_cpst_id unique (mtdt_id, user_dfn_tbl_cpst_id);
    end if;
end $$;

create unique index if not exists uq_t_mtdt_tbl_ctgr_root_name
    on t_mtdt_tbl_ctgr (mtdt_id, ctgr_nm)
    where up_mtdt_tbl_ctgr_id is null;

create unique index if not exists uq_t_mtdt_tbl_ctgr_mpng_mtdt_tbl
    on t_mtdt_tbl_ctgr_mpng (mtdt_id, mtdt_tbl_id);

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'fk_t_mtdt_tbl_ctgr_parent_same_mtdt') then
        alter table t_mtdt_tbl_ctgr
            add constraint fk_t_mtdt_tbl_ctgr_parent_same_mtdt
                foreign key (mtdt_id, up_mtdt_tbl_ctgr_id)
                references t_mtdt_tbl_ctgr (mtdt_id, mtdt_tbl_ctgr_id)
                on delete cascade;
    end if;

    if not exists (select 1 from pg_constraint where conname = 'fk_t_mtdt_tbl_ctgr_mpng_ctgr_same_mtdt') then
        alter table t_mtdt_tbl_ctgr_mpng
            add constraint fk_t_mtdt_tbl_ctgr_mpng_ctgr_same_mtdt
                foreign key (mtdt_id, mtdt_tbl_ctgr_id)
                references t_mtdt_tbl_ctgr (mtdt_id, mtdt_tbl_ctgr_id)
                on delete cascade;
    end if;

    if not exists (select 1 from pg_constraint where conname = 'fk_t_mtdt_tbl_ctgr_mpng_tbl_same_mtdt') then
        alter table t_mtdt_tbl_ctgr_mpng
            add constraint fk_t_mtdt_tbl_ctgr_mpng_tbl_same_mtdt
                foreign key (mtdt_id, mtdt_tbl_id)
                references t_mtdt_tbl (mtdt_id, mtdt_tbl_id)
                on delete cascade;
    end if;

    if not exists (select 1 from pg_constraint where conname = 'fk_t_cd_type_mtdt_tbl_same_mtdt') then
        alter table t_cd_type
            add constraint fk_t_cd_type_mtdt_tbl_same_mtdt
                foreign key (mtdt_id, mtdt_tbl_id)
                references t_mtdt_tbl (mtdt_id, mtdt_tbl_id)
                on delete cascade;
    end if;

    if not exists (select 1 from pg_constraint where conname = 'fk_t_mtdt_col_mtdt_tbl_same_mtdt') then
        alter table t_mtdt_col
            add constraint fk_t_mtdt_col_mtdt_tbl_same_mtdt
                foreign key (mtdt_id, mtdt_tbl_id)
                references t_mtdt_tbl (mtdt_id, mtdt_tbl_id)
                on delete cascade;
    end if;

    if not exists (select 1 from pg_constraint where conname = 'fk_t_mtdt_col_cd_type_same_mtdt') then
        alter table t_mtdt_col
            add constraint fk_t_mtdt_col_cd_type_same_mtdt
                foreign key (mtdt_id, cd_type_id)
                references t_cd_type (mtdt_id, cd_type_id)
                on delete set null (cd_type_id);
    end if;

    if not exists (select 1 from pg_constraint where conname = 'fk_t_mtdt_col_src_col_same_mtdt') then
        alter table t_mtdt_col
            add constraint fk_t_mtdt_col_src_col_same_mtdt
                foreign key (mtdt_id, src_mtdt_col_id)
                references t_mtdt_col (mtdt_id, mtdt_col_id)
                on delete set null (src_mtdt_col_id);
    end if;

    if not exists (select 1 from pg_constraint where conname = 'fk_t_mtdt_user_dfn_tbl_cpst_user_tbl_same_mtdt') then
        alter table t_mtdt_user_dfn_tbl_cpst
            add constraint fk_t_mtdt_user_dfn_tbl_cpst_user_tbl_same_mtdt
                foreign key (mtdt_id, user_dfn_mtdt_tbl_id)
                references t_mtdt_tbl (mtdt_id, mtdt_tbl_id)
                on delete cascade;
    end if;

    if not exists (select 1 from pg_constraint where conname = 'fk_t_mtdt_user_dfn_tbl_cpst_src_tbl_same_mtdt') then
        alter table t_mtdt_user_dfn_tbl_cpst
            add constraint fk_t_mtdt_user_dfn_tbl_cpst_src_tbl_same_mtdt
                foreign key (mtdt_id, src_mtdt_tbl_id)
                references t_mtdt_tbl (mtdt_id, mtdt_tbl_id)
                on delete cascade;
    end if;

    if not exists (select 1 from pg_constraint where conname = 'fk_t_mtdt_user_dfn_tbl_cpst_up_cpst_same_mtdt') then
        alter table t_mtdt_user_dfn_tbl_cpst
            add constraint fk_t_mtdt_user_dfn_tbl_cpst_up_cpst_same_mtdt
                foreign key (mtdt_id, up_cpst_id)
                references t_mtdt_user_dfn_tbl_cpst (mtdt_id, user_dfn_tbl_cpst_id)
                on delete cascade;
    end if;

    if not exists (select 1 from pg_constraint where conname = 'fk_t_mtdt_user_dfn_tbl_cpst_up_join_col_same_mtdt') then
        alter table t_mtdt_user_dfn_tbl_cpst
            add constraint fk_t_mtdt_user_dfn_tbl_cpst_up_join_col_same_mtdt
                foreign key (mtdt_id, up_jn_mtdt_col_id)
                references t_mtdt_col (mtdt_id, mtdt_col_id)
                on delete set null (up_jn_mtdt_col_id);
    end if;

    if not exists (select 1 from pg_constraint where conname = 'fk_t_mtdt_user_dfn_tbl_cpst_src_join_col_same_mtdt') then
        alter table t_mtdt_user_dfn_tbl_cpst
            add constraint fk_t_mtdt_user_dfn_tbl_cpst_src_join_col_same_mtdt
                foreign key (mtdt_id, src_jn_mtdt_col_id)
                references t_mtdt_col (mtdt_id, mtdt_col_id)
                on delete set null (src_jn_mtdt_col_id);
    end if;

    if not exists (select 1 from pg_constraint where conname = 'fk_t_mtdt_col_src_user_dfn_cpst_same_mtdt') then
        alter table t_mtdt_col
            add constraint fk_t_mtdt_col_src_user_dfn_cpst_same_mtdt
                foreign key (mtdt_id, src_user_dfn_tbl_cpst_id)
                references t_mtdt_user_dfn_tbl_cpst (mtdt_id, user_dfn_tbl_cpst_id)
                on delete set null (src_user_dfn_tbl_cpst_id);
    end if;
end $$;

create table t_extr_spcf (
    extr_spcf_id varchar(36) primary key,
    spcf_psn_se_cd varchar(32) not null,
    mtdt_id varchar(36) not null,
    mtdt_tbl_id varchar(36) not null,
    fltr_lgc_cd varchar(16) not null,
    dpcn_prm_yn boolean not null default false,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_extr_spcf_mtdt_set
        foreign key (mtdt_id) references t_mtdt_set (mtdt_id),
    constraint fk_t_extr_spcf_mtdt_tbl
        foreign key (mtdt_id, mtdt_tbl_id) references t_mtdt_tbl (mtdt_id, mtdt_tbl_id),
    constraint ck_t_extr_spcf_psn_se
        check (spcf_psn_se_cd in ('FORM', 'REQUEST')),
    constraint ck_t_extr_spcf_fltr_lgc
        check (fltr_lgc_cd in ('AND', 'OR'))
);

create table t_extr_spcf_col (
    extr_spcf_col_id varchar(36) primary key,
    extr_spcf_id varchar(36) not null,
    mtdt_col_id varchar(36) not null,
    sort_no integer not null,
    cd_nm_incl_yn boolean not null default false,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_extr_spcf_col_spec
        foreign key (extr_spcf_id) references t_extr_spcf (extr_spcf_id)
            on delete cascade,
    constraint fk_t_extr_spcf_col_mtdt_col
        foreign key (mtdt_col_id) references t_mtdt_col (mtdt_col_id)
            on delete cascade
);

create table t_extr_spcf_fltr (
    extr_spcf_fltr_id varchar(36) primary key,
    extr_spcf_id varchar(36) not null,
    mtdt_col_id varchar(36) not null,
    fltr_cnd_cd varchar(32) not null,
    fltr_lgc_cd varchar(16),
    vl_cn text not null,
    sort_no integer not null,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_extr_spcf_fltr_spec
        foreign key (extr_spcf_id) references t_extr_spcf (extr_spcf_id)
            on delete cascade,
    constraint fk_t_extr_spcf_fltr_mtdt_col
        foreign key (mtdt_col_id) references t_mtdt_col (mtdt_col_id)
            on delete cascade,
    constraint ck_t_extr_spcf_fltr_cnd
        check (fltr_cnd_cd in ('EQ', 'NE', 'GT', 'GTE', 'LT', 'LTE', 'LIKE', 'STARTS_WITH', 'ENDS_WITH', 'IN', 'NOT_IN', 'BETWEEN', 'IS_NULL', 'IS_NOT_NULL')),
    constraint ck_t_extr_spcf_fltr_lgc
        check (fltr_lgc_cd is null or fltr_lgc_cd in ('AND', 'OR'))
);

create table t_extr_spcf_sort (
    extr_spcf_sort_id varchar(36) primary key,
    extr_spcf_id varchar(36) not null,
    mtdt_col_id varchar(36) not null,
    sort_drctn_cd varchar(16) not null,
    prrty_rnk integer not null,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_extr_spcf_sort_spec
        foreign key (extr_spcf_id) references t_extr_spcf (extr_spcf_id)
            on delete cascade,
    constraint fk_t_extr_spcf_sort_mtdt_col
        foreign key (mtdt_col_id) references t_mtdt_col (mtdt_col_id)
            on delete cascade,
    constraint ck_t_extr_spcf_sort_drctn
        check (sort_drctn_cd in ('ASC', 'DESC')),
    constraint ck_t_extr_spcf_sort_prrty
        check (prrty_rnk > 0)
);

create table t_extr_form (
    extr_form_id varchar(36) primary key,
    extr_spcf_id varchar(36) not null,
    extr_form_nm varchar(200) not null,
    extr_form_expln varchar(1000),
    use_yn boolean not null,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_extr_form_spec
        foreign key (extr_spcf_id) references t_extr_spcf (extr_spcf_id)
);

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
    mdfcn_dt timestamp not null,
    constraint fk_t_extr_dmnd_form
        foreign key (extr_form_id) references t_extr_form (extr_form_id)
            on delete set null,
    constraint ck_t_extr_dmnd_src
        check (dmnd_src_cd in ('EXTERNAL_API', 'ADMIN_PLATFORM')),
    constraint ck_t_extr_dmnd_stts
        check (extr_dmnd_stts_cd in ('READY', 'RUNNING', 'PREPARED', 'APPROVED', 'EXPORTING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

create table t_extr_datst (
    extr_datst_id varchar(36) primary key,
    extr_dmnd_id varchar(36) not null,
    extr_spcf_id varchar(36) not null,
    datst_nm varchar(255) not null,
    extr_datst_stts_cd varchar(32) not null,
    sort_no integer not null,
    creatr_id varchar(120) not null,
    mdfr_id varchar(120) not null,
    crt_dt timestamp not null,
    mdfcn_dt timestamp not null,
    constraint fk_t_extr_datst_dmnd
        foreign key (extr_dmnd_id) references t_extr_dmnd (extr_dmnd_id)
            on delete cascade,
    constraint fk_t_extr_datst_spec
        foreign key (extr_spcf_id) references t_extr_spcf (extr_spcf_id),
    constraint ck_t_extr_datst_stts
        check (extr_datst_stts_cd in ('READY', 'RUNNING', 'PREPARED', 'APPROVED', 'EXPORTING', 'COMPLETED', 'FAILED', 'CANCELLED'))
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
    constraint fk_t_extr_excn_datst
        foreign key (extr_datst_id) references t_extr_datst (extr_datst_id)
            on delete cascade,
    constraint uq_t_extr_excn_datst_sn
        unique (extr_datst_id, excn_sn),
    constraint ck_t_extr_excn_type
        check (excn_type_cd in ('PREPARE', 'EXPORT', 'CLEANUP')),
    constraint ck_t_extr_excn_stts
        check (excn_stts_cd in ('RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED')),
    constraint ck_t_extr_excn_rslt_type
        check (rslt_strg_type_cd is null or rslt_strg_type_cd in ('TABLE', 'FILE', 'PARQUET'))
);

create index idx_t_extr_spcf_mtdt_tbl
    on t_extr_spcf (mtdt_id, mtdt_tbl_id);

create index idx_t_extr_form_use_yn_crt_dt
    on t_extr_form (use_yn, crt_dt desc);

create index idx_t_extr_form_spec
    on t_extr_form (extr_spcf_id);

create index idx_t_extr_spcf_col_spec
    on t_extr_spcf_col (extr_spcf_id, sort_no);

create index idx_t_extr_spcf_fltr_spec
    on t_extr_spcf_fltr (extr_spcf_id, sort_no);

create index idx_t_extr_spcf_sort_spec
    on t_extr_spcf_sort (extr_spcf_id, prrty_rnk);

create index idx_t_extr_dmnd_stts
    on t_extr_dmnd (extr_dmnd_stts_cd);

create index idx_t_extr_dmnd_src
    on t_extr_dmnd (dmnd_src_cd);

create index idx_t_extr_dmnd_otsd_id
    on t_extr_dmnd (otsd_dmnd_id);

create index idx_t_extr_datst_dmnd
    on t_extr_datst (extr_dmnd_id, sort_no);

create index idx_t_extr_datst_spec
    on t_extr_datst (extr_spcf_id);

create index idx_t_extr_excn_datst
    on t_extr_excn (extr_datst_id, excn_sn desc);

create index idx_t_extr_excn_datst_type
    on t_extr_excn (extr_datst_id, excn_type_cd, excn_sn desc);

create index idx_t_extr_excn_job
    on t_extr_excn (extr_job_id)
    where extr_job_id is not null;

comment on table t_extr_spcf is '추출스펙';
comment on column t_extr_spcf.extr_spcf_id is '추출스펙아이디';
comment on column t_extr_spcf.spcf_psn_se_cd is '스펙소유구분코드';
comment on column t_extr_spcf.mtdt_id is '메타데이터아이디';
comment on column t_extr_spcf.mtdt_tbl_id is '메타데이터테이블아이디';
comment on column t_extr_spcf.fltr_lgc_cd is '필터논리코드';
comment on column t_extr_spcf.dpcn_prm_yn is 'Allow duplicate rows during extraction';
comment on column t_extr_spcf.creatr_id is '생성자아이디';
comment on column t_extr_spcf.mdfr_id is '수정자아이디';
comment on column t_extr_spcf.crt_dt is '생성일시';
comment on column t_extr_spcf.mdfcn_dt is '수정일시';

comment on table t_extr_spcf_col is '추출스펙선택컬럼';
comment on column t_extr_spcf_col.extr_spcf_col_id is '추출스펙선택컬럼아이디';
comment on column t_extr_spcf_col.extr_spcf_id is '추출스펙아이디';
comment on column t_extr_spcf_col.mtdt_col_id is '메타데이터컬럼아이디';
comment on column t_extr_spcf_col.sort_no is '정렬번호';
comment on column t_extr_spcf_col.cd_nm_incl_yn is '코드명포함여부';
comment on column t_extr_spcf_col.creatr_id is '생성자아이디';
comment on column t_extr_spcf_col.mdfr_id is '수정자아이디';
comment on column t_extr_spcf_col.crt_dt is '생성일시';
comment on column t_extr_spcf_col.mdfcn_dt is '수정일시';

comment on table t_extr_spcf_fltr is '추출스펙필터';
comment on column t_extr_spcf_fltr.extr_spcf_fltr_id is '추출스펙필터아이디';
comment on column t_extr_spcf_fltr.extr_spcf_id is '추출스펙아이디';
comment on column t_extr_spcf_fltr.mtdt_col_id is '메타데이터컬럼아이디';
comment on column t_extr_spcf_fltr.fltr_cnd_cd is '필터조건코드';
comment on column t_extr_spcf_fltr.fltr_lgc_cd is 'Previous filter connector logic code';
comment on column t_extr_spcf_fltr.vl_cn is '값내용';
comment on column t_extr_spcf_fltr.sort_no is '정렬번호';
comment on column t_extr_spcf_fltr.creatr_id is '생성자아이디';
comment on column t_extr_spcf_fltr.mdfr_id is '수정자아이디';
comment on column t_extr_spcf_fltr.crt_dt is '생성일시';
comment on column t_extr_spcf_fltr.mdfcn_dt is '수정일시';

comment on table t_extr_spcf_sort is '추출스펙정렬';
comment on column t_extr_spcf_sort.extr_spcf_sort_id is '추출스펙정렬아이디';
comment on column t_extr_spcf_sort.extr_spcf_id is '추출스펙아이디';
comment on column t_extr_spcf_sort.mtdt_col_id is '메타데이터컬럼아이디';
comment on column t_extr_spcf_sort.sort_drctn_cd is '정렬방향코드';
comment on column t_extr_spcf_sort.prrty_rnk is '우선순위';
comment on column t_extr_spcf_sort.creatr_id is '생성자아이디';
comment on column t_extr_spcf_sort.mdfr_id is '수정자아이디';
comment on column t_extr_spcf_sort.crt_dt is '생성일시';
comment on column t_extr_spcf_sort.mdfcn_dt is '수정일시';

comment on table t_extr_form is '추출양식';
comment on column t_extr_form.extr_form_id is '추출양식아이디';
comment on column t_extr_form.extr_spcf_id is '추출스펙아이디';
comment on column t_extr_form.extr_form_nm is '추출양식명';
comment on column t_extr_form.extr_form_expln is '추출양식설명';
comment on column t_extr_form.use_yn is '사용여부';
comment on column t_extr_form.creatr_id is '생성자아이디';
comment on column t_extr_form.mdfr_id is '수정자아이디';
comment on column t_extr_form.crt_dt is '생성일시';
comment on column t_extr_form.mdfcn_dt is '수정일시';

comment on table t_extr_dmnd is '추출요청';
comment on column t_extr_dmnd.extr_dmnd_id is '추출요청아이디';
comment on column t_extr_dmnd.dmnd_src_cd is '요청출처코드';
comment on column t_extr_dmnd.extr_form_id is '추출양식아이디';
comment on column t_extr_dmnd.extr_dmnd_nm is '추출요청명';
comment on column t_extr_dmnd.extr_dmnd_stts_cd is '추출요청상태코드';
comment on column t_extr_dmnd.otsd_dmnd_id is '외부요청아이디';
comment on column t_extr_dmnd.datst_cnt is '데이터셋수';
comment on column t_extr_dmnd.creatr_id is '생성자아이디';
comment on column t_extr_dmnd.mdfr_id is '수정자아이디';
comment on column t_extr_dmnd.crt_dt is '생성일시';
comment on column t_extr_dmnd.mdfcn_dt is '수정일시';

comment on table t_extr_datst is '추출데이터셋';
comment on column t_extr_datst.extr_datst_id is '추출데이터셋아이디';
comment on column t_extr_datst.extr_dmnd_id is '추출요청아이디';
comment on column t_extr_datst.extr_spcf_id is '추출스펙아이디';
comment on column t_extr_datst.datst_nm is '데이터셋명';
comment on column t_extr_datst.extr_datst_stts_cd is '추출데이터셋상태코드';
comment on column t_extr_datst.sort_no is '정렬번호';
comment on column t_extr_datst.creatr_id is '생성자아이디';
comment on column t_extr_datst.mdfr_id is '수정자아이디';
comment on column t_extr_datst.crt_dt is '생성일시';
comment on column t_extr_datst.mdfcn_dt is '수정일시';

comment on table t_extr_excn is '추출실행';
comment on column t_extr_excn.extr_excn_id is '추출실행아이디';
comment on column t_extr_excn.extr_datst_id is '추출데이터셋아이디';
comment on column t_extr_excn.excn_sn is '실행일련번호';
comment on column t_extr_excn.excn_type_cd is 'Execution type: PREPARE, EXPORT, or CLEANUP';
comment on column t_extr_excn.excn_stts_cd is '실행상태코드';
comment on column t_extr_excn.extr_job_id is '추출작업아이디';
comment on column t_extr_excn.excn_bgng_dt is '실행시작일시';
comment on column t_extr_excn.excn_end_dt is '실행종료일시';
comment on column t_extr_excn.rslt_nocs is '결과건수';
comment on column t_extr_excn.rslt_strg_type_cd is '결과저장유형코드';
comment on column t_extr_excn.rslt_tbl_nm is '결과테이블명';
comment on column t_extr_excn.rslt_file_path is '결과파일경로';
comment on column t_extr_excn.fail_cn is '실패내용';
comment on column t_extr_excn.checkpoint_state is 'Worker prepare checkpoint state';
comment on column t_extr_excn.resumed_from_checkpoint is 'Whether this run resumed from a worker checkpoint';
comment on column t_extr_excn.resume_message is 'Worker checkpoint resume message';
comment on column t_extr_excn.excn_sql_cn is '실행SQL내용';
comment on column t_extr_excn.excn_sql_vrbl_cn is '실행SQL변수내용';
comment on column t_extr_excn.excn_req_cn is 'Execution request payload JSON';
comment on column t_extr_excn.creatr_id is '생성자아이디';
comment on column t_extr_excn.mdfr_id is '수정자아이디';
comment on column t_extr_excn.crt_dt is '생성일시';
comment on column t_extr_excn.mdfcn_dt is '수정일시';

