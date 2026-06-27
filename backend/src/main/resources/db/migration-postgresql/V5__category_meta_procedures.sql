create or replace procedure sp_mtdt_set(
    p_op char(1), inout p_mtdt_id varchar,
    p_lnkg_id varchar default null, p_mtdt_nm varchar default null, p_mtdt_expln varchar default null,
    p_last_sync_dt timestamp default null, p_last_sync_stts_cd varchar default null, p_last_sync_msg_cn varchar default null,
    p_actor_id varchar default 'system'
)
language plpgsql as $$
begin
    if p_op = 'C' then
        if p_mtdt_id is null or trim(p_mtdt_id) = '' then
            p_mtdt_id := fn_next_etl_id('mtdt');
        end if;
        insert into t_mtdt_set (
            mtdt_id, lnkg_id, mtdt_nm, mtdt_expln, last_sync_dt, last_sync_stts_cd, last_sync_msg_cn,
            creatr_id, mdfr_id, crt_dt, mdfcn_dt
        ) values (
            p_mtdt_id, p_lnkg_id, p_mtdt_nm, p_mtdt_expln, p_last_sync_dt,
            coalesce(p_last_sync_stts_cd, 'NEVER'), p_last_sync_msg_cn,
            p_actor_id, p_actor_id, current_timestamp, current_timestamp
        );
    elsif p_op = 'U' then
        update t_mtdt_set set
            lnkg_id = coalesce(p_lnkg_id, lnkg_id),
            mtdt_nm = coalesce(p_mtdt_nm, mtdt_nm),
            mtdt_expln = coalesce(p_mtdt_expln, mtdt_expln),
            last_sync_dt = coalesce(p_last_sync_dt, last_sync_dt),
            last_sync_stts_cd = coalesce(p_last_sync_stts_cd, last_sync_stts_cd),
            last_sync_msg_cn = coalesce(p_last_sync_msg_cn, last_sync_msg_cn),
            mdfr_id = p_actor_id,
            mdfcn_dt = current_timestamp
        where mtdt_id = p_mtdt_id;
    elsif p_op = 'D' then
        delete from t_mtdt_set where mtdt_id = p_mtdt_id;
    else
        raise exception '지원하지 않는 Operation: %', p_op;
    end if;
end;
$$;

create or replace procedure sp_mtdt_tbl(
    p_op char(1), inout p_mtdt_tbl_id varchar,
    p_mtdt_id varchar default null, p_orgnl_tbl_nm varchar default null, p_tbl_nm varchar default null,
    p_tbl_expln varchar default null, p_orgnl_tbl_expln varchar default null,
    p_sort_no integer default null, p_expsr_yn boolean default null, p_src_exst_yn boolean default null,
    p_use_yn boolean default null, p_cd_tbl_yn boolean default null, p_tbl_type_cd varchar default null,
    p_actor_id varchar default 'system'
)
language plpgsql as $$
begin
    if p_op = 'C' then
        if p_mtdt_tbl_id is null or trim(p_mtdt_tbl_id) = '' then
            p_mtdt_tbl_id := fn_next_etl_id('mtbl');
        end if;
        insert into t_mtdt_tbl (
            mtdt_tbl_id, mtdt_id, orgnl_tbl_nm, tbl_nm, tbl_expln, orgnl_tbl_expln,
            sort_no, expsr_yn, src_exst_yn, use_yn, cd_tbl_yn, tbl_type_cd,
            creatr_id, mdfr_id, crt_dt, mdfcn_dt
        ) values (
            p_mtdt_tbl_id, p_mtdt_id, p_orgnl_tbl_nm, p_tbl_nm, p_tbl_expln, p_orgnl_tbl_expln,
            coalesce(p_sort_no, 0), coalesce(p_expsr_yn, true), coalesce(p_src_exst_yn, true),
            coalesce(p_use_yn, true), coalesce(p_cd_tbl_yn, false), coalesce(p_tbl_type_cd, 'SOURCE'),
            p_actor_id, p_actor_id, current_timestamp, current_timestamp
        );
    elsif p_op = 'U' then
        update t_mtdt_tbl set
            orgnl_tbl_nm = coalesce(p_orgnl_tbl_nm, orgnl_tbl_nm),
            tbl_nm = coalesce(p_tbl_nm, tbl_nm),
            tbl_expln = coalesce(p_tbl_expln, tbl_expln),
            orgnl_tbl_expln = coalesce(p_orgnl_tbl_expln, orgnl_tbl_expln),
            sort_no = coalesce(p_sort_no, sort_no),
            expsr_yn = coalesce(p_expsr_yn, expsr_yn),
            src_exst_yn = coalesce(p_src_exst_yn, src_exst_yn),
            use_yn = coalesce(p_use_yn, use_yn),
            cd_tbl_yn = coalesce(p_cd_tbl_yn, cd_tbl_yn),
            tbl_type_cd = coalesce(p_tbl_type_cd, tbl_type_cd),
            mdfr_id = p_actor_id,
            mdfcn_dt = current_timestamp
        where mtdt_tbl_id = p_mtdt_tbl_id;
    elsif p_op = 'D' then
        update t_mtdt_tbl set use_yn = false, mdfr_id = p_actor_id, mdfcn_dt = current_timestamp
        where mtdt_tbl_id = p_mtdt_tbl_id;
    else
        raise exception '지원하지 않는 Operation: %', p_op;
    end if;
end;
$$;

create or replace procedure sp_mtdt_tbl_ctgr(
    p_op char(1), inout p_mtdt_tbl_ctgr_id varchar,
    p_mtdt_id varchar default null, p_up_mtdt_tbl_ctgr_id varchar default null,
    p_ctgr_nm varchar default null, p_ctgr_expln varchar default null,
    p_sort_no integer default null, p_expsr_yn boolean default null, p_lwr_ctgr_prm_yn boolean default null,
    p_actor_id varchar default 'system'
)
language plpgsql as $$
begin
    if p_op = 'C' then
        if p_mtdt_tbl_ctgr_id is null or trim(p_mtdt_tbl_ctgr_id) = '' then
            p_mtdt_tbl_ctgr_id := fn_next_etl_id('ctgr');
        end if;
        insert into t_mtdt_tbl_ctgr (
            mtdt_tbl_ctgr_id, mtdt_id, up_mtdt_tbl_ctgr_id, ctgr_nm, ctgr_expln,
            sort_no, expsr_yn, lwr_ctgr_prm_yn, creatr_id, mdfr_id, crt_dt, mdfcn_dt
        ) values (
            p_mtdt_tbl_ctgr_id, p_mtdt_id, p_up_mtdt_tbl_ctgr_id, p_ctgr_nm, p_ctgr_expln,
            coalesce(p_sort_no, 0), coalesce(p_expsr_yn, true), coalesce(p_lwr_ctgr_prm_yn, true),
            p_actor_id, p_actor_id, current_timestamp, current_timestamp
        );
    elsif p_op = 'U' then
        update t_mtdt_tbl_ctgr set
            up_mtdt_tbl_ctgr_id = coalesce(p_up_mtdt_tbl_ctgr_id, up_mtdt_tbl_ctgr_id),
            ctgr_nm = coalesce(p_ctgr_nm, ctgr_nm),
            ctgr_expln = coalesce(p_ctgr_expln, ctgr_expln),
            sort_no = coalesce(p_sort_no, sort_no),
            expsr_yn = coalesce(p_expsr_yn, expsr_yn),
            lwr_ctgr_prm_yn = coalesce(p_lwr_ctgr_prm_yn, lwr_ctgr_prm_yn),
            mdfr_id = p_actor_id,
            mdfcn_dt = current_timestamp
        where mtdt_tbl_ctgr_id = p_mtdt_tbl_ctgr_id;
    elsif p_op = 'D' then
        delete from t_mtdt_tbl_ctgr where mtdt_tbl_ctgr_id = p_mtdt_tbl_ctgr_id;
    else
        raise exception '지원하지 않는 Operation: %', p_op;
    end if;
end;
$$;

create or replace procedure sp_mtdt_tbl_ctgr_mpng(
    p_op char(1), inout p_mtdt_tbl_ctgr_mpng_id varchar,
    p_mtdt_id varchar default null, p_mtdt_tbl_ctgr_id varchar default null, p_mtdt_tbl_id varchar default null,
    p_sort_no integer default null, p_expsr_yn boolean default null,
    p_actor_id varchar default 'system'
)
language plpgsql as $$
begin
    if p_op = 'C' then
        if p_mtdt_tbl_ctgr_mpng_id is null or trim(p_mtdt_tbl_ctgr_mpng_id) = '' then
            p_mtdt_tbl_ctgr_mpng_id := fn_next_etl_id('mpng');
        end if;
        insert into t_mtdt_tbl_ctgr_mpng (
            mtdt_tbl_ctgr_mpng_id, mtdt_id, mtdt_tbl_ctgr_id, mtdt_tbl_id,
            sort_no, expsr_yn, creatr_id, mdfr_id, crt_dt, mdfcn_dt
        ) values (
            p_mtdt_tbl_ctgr_mpng_id, p_mtdt_id, p_mtdt_tbl_ctgr_id, p_mtdt_tbl_id,
            coalesce(p_sort_no, 0), coalesce(p_expsr_yn, true),
            p_actor_id, p_actor_id, current_timestamp, current_timestamp
        );
    elsif p_op = 'U' then
        update t_mtdt_tbl_ctgr_mpng set
            mtdt_tbl_ctgr_id = coalesce(p_mtdt_tbl_ctgr_id, mtdt_tbl_ctgr_id),
            mtdt_tbl_id = coalesce(p_mtdt_tbl_id, mtdt_tbl_id),
            sort_no = coalesce(p_sort_no, sort_no),
            expsr_yn = coalesce(p_expsr_yn, expsr_yn),
            mdfr_id = p_actor_id,
            mdfcn_dt = current_timestamp
        where mtdt_tbl_ctgr_mpng_id = p_mtdt_tbl_ctgr_mpng_id;
    elsif p_op = 'D' then
        delete from t_mtdt_tbl_ctgr_mpng where mtdt_tbl_ctgr_mpng_id = p_mtdt_tbl_ctgr_mpng_id;
    else
        raise exception '지원하지 않는 Operation: %', p_op;
    end if;
end;
$$;

comment on procedure sp_mtdt_set(character, inout character varying, character varying, character varying, character varying, timestamp without time zone, character varying, character varying, character varying) is '메타데이터세트 CUD. op: C=등록(mtdt 접두어 자동 채번), U=부분 수정, D=물리 삭제';

comment on procedure sp_mtdt_tbl(character, inout character varying, character varying, character varying, character varying, character varying, character varying, integer, boolean, boolean, boolean, boolean, character varying, character varying) is '메타데이터테이블 CUD. op: C=등록(mtbl 접두어 자동 채번), U=부분 수정, D=논리삭제(use_yn=false)';

comment on procedure sp_mtdt_tbl_ctgr(character, inout character varying, character varying, character varying, character varying, character varying, integer, boolean, boolean, character varying) is '메타데이터테이블카테고리 CUD. op: C=등록(ctgr 접두어 자동 채번), U=부분 수정, D=물리 삭제';

comment on procedure sp_mtdt_tbl_ctgr_mpng(character, inout character varying, character varying, character varying, character varying, integer, boolean, character varying) is '메타데이터테이블카테고리매핑 CUD. op: C=등록(mpng 접두어 자동 채번), U=부분 수정, D=물리 삭제';
