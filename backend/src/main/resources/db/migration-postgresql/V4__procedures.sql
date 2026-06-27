create or replace function fn_next_etl_id(p_prefix varchar)
returns varchar
language plpgsql
as $$
declare
    v_no integer;
begin
    update t_etl_seq
    set last_no = last_no + 1
    where seq_key = upper(p_prefix)
      and seq_dt = current_date
    returning last_no into v_no;

    if not found then
        insert into t_etl_seq (seq_key, seq_dt, last_no)
        values (upper(p_prefix), current_date, 1)
        returning last_no into v_no;
    end if;

    return lower(p_prefix) || '-' || to_char(current_date, 'YYYYMMDD') || '-' || lpad(v_no::text, 3, '0');
end;
$$;

create or replace procedure sp_lnkg_profile(
    p_op char(1), inout p_lnkg_id varchar,
    p_lnkg_nm varchar default null, p_db_type_cd varchar default null,
    p_srvr_addr varchar default null, p_srvr_port_no integer default null,
    p_db_nm varchar default null, p_schm_nm varchar default null,
    p_lnkg_expln varchar default null, p_acnt_id varchar default null,
    p_enpswd text default null, p_use_yn boolean default null,
    p_lnkg_stts_cd varchar default null, p_test_rslt_cd varchar default null,
    p_last_test_dt timestamp default null, p_actor_id varchar default 'system'
)
language plpgsql as $$
begin
    if p_op = 'C' then
        if p_lnkg_id is null or trim(p_lnkg_id) = '' then
            p_lnkg_id := fn_next_etl_id('lnkg');
        end if;
        insert into t_lnkg_profile (
            lnkg_id, lnkg_nm, db_type_cd, srvr_addr, srvr_port_no, db_nm, schm_nm,
            lnkg_expln, acnt_id, enpswd, use_yn, lnkg_stts_cd, test_rslt_cd, last_test_dt,
            creatr_id, mdfr_id, crt_dt, mdfcn_dt
        ) values (
            p_lnkg_id, p_lnkg_nm, p_db_type_cd, p_srvr_addr, p_srvr_port_no, p_db_nm, p_schm_nm,
            p_lnkg_expln, p_acnt_id, p_enpswd, coalesce(p_use_yn, true),
            coalesce(p_lnkg_stts_cd, 'ACTIVE'), coalesce(p_test_rslt_cd, 'VALIDATING'), p_last_test_dt,
            p_actor_id, p_actor_id, current_timestamp, current_timestamp
        );
    elsif p_op = 'U' then
        update t_lnkg_profile set
            lnkg_nm = coalesce(p_lnkg_nm, lnkg_nm), db_type_cd = coalesce(p_db_type_cd, db_type_cd),
            srvr_addr = coalesce(p_srvr_addr, srvr_addr), srvr_port_no = coalesce(p_srvr_port_no, srvr_port_no),
            db_nm = coalesce(p_db_nm, db_nm), schm_nm = coalesce(p_schm_nm, schm_nm),
            lnkg_expln = coalesce(p_lnkg_expln, lnkg_expln), acnt_id = coalesce(p_acnt_id, acnt_id),
            enpswd = coalesce(p_enpswd, enpswd), use_yn = coalesce(p_use_yn, use_yn),
            lnkg_stts_cd = coalesce(p_lnkg_stts_cd, lnkg_stts_cd), test_rslt_cd = coalesce(p_test_rslt_cd, test_rslt_cd),
            last_test_dt = coalesce(p_last_test_dt, last_test_dt), mdfr_id = p_actor_id, mdfcn_dt = current_timestamp
        where lnkg_id = p_lnkg_id;
    elsif p_op = 'D' then
        update t_lnkg_profile set use_yn = false, lnkg_stts_cd = 'DELETED', mdfr_id = p_actor_id, mdfcn_dt = current_timestamp
        where lnkg_id = p_lnkg_id;
    else
        raise exception '지원하지 않는 Operation: %', p_op;
    end if;
end;
$$;

create or replace procedure sp_mig_job(
    p_op char(1), inout p_mig_job_id varchar,
    p_src_lnkg_id varchar default null, p_trgt_lnkg_id varchar default null, p_mtdt_id varchar default null,
    p_src_schm_nm varchar default null, p_trgt_schm_nm varchar default null,
    p_batch_sz integer default null, p_drop_exst_yn boolean default null,
    p_job_stts_cd varchar default null, p_tot_tbl_cnt integer default null,
    p_succ_tbl_cnt integer default null, p_fail_tbl_cnt integer default null,
    p_tot_row_cnt bigint default null, p_err_msg_cn varchar default null,
    p_bgng_dt timestamp default null, p_end_dt timestamp default null,
    p_actor_id varchar default 'system'
)
language plpgsql as $$
begin
    if p_op = 'C' then
        if p_mig_job_id is null or trim(p_mig_job_id) = '' then
            p_mig_job_id := fn_next_etl_id('etl');
        end if;
        insert into t_mig_job (
            mig_job_id, src_lnkg_id, trgt_lnkg_id, mtdt_id, src_schm_nm, trgt_schm_nm,
            batch_sz, drop_exst_yn, job_stts_cd, tot_tbl_cnt, succ_tbl_cnt, fail_tbl_cnt, tot_row_cnt,
            creatr_id, mdfr_id, crt_dt, mdfcn_dt
        ) values (
            p_mig_job_id, p_src_lnkg_id, p_trgt_lnkg_id, p_mtdt_id, p_src_schm_nm, p_trgt_schm_nm,
            coalesce(p_batch_sz, 500), coalesce(p_drop_exst_yn, true), coalesce(p_job_stts_cd, 'PENDING'),
            coalesce(p_tot_tbl_cnt, 0), 0, 0, 0, p_actor_id, p_actor_id, current_timestamp, current_timestamp
        );
    elsif p_op = 'U' then
        update t_mig_job set
            job_stts_cd = coalesce(p_job_stts_cd, job_stts_cd), tot_tbl_cnt = coalesce(p_tot_tbl_cnt, tot_tbl_cnt),
            succ_tbl_cnt = coalesce(p_succ_tbl_cnt, succ_tbl_cnt), fail_tbl_cnt = coalesce(p_fail_tbl_cnt, fail_tbl_cnt),
            tot_row_cnt = coalesce(p_tot_row_cnt, tot_row_cnt), err_msg_cn = coalesce(p_err_msg_cn, err_msg_cn),
            bgng_dt = coalesce(p_bgng_dt, bgng_dt), end_dt = coalesce(p_end_dt, end_dt),
            mdfr_id = p_actor_id, mdfcn_dt = current_timestamp
        where mig_job_id = p_mig_job_id;
    elsif p_op = 'D' then
        delete from t_mig_job where mig_job_id = p_mig_job_id;
    else
        raise exception '지원하지 않는 Operation: %', p_op;
    end if;
end;
$$;

create or replace procedure sp_mig_job_tbl(
    p_op char(1), inout p_mig_job_tbl_id varchar,
    p_mig_job_id varchar default null, p_tbl_nm varchar default null,
    p_tbl_stts_cd varchar default null, p_row_cnt bigint default null, p_batch_cnt integer default null,
    p_crt_tbl_ddl_cn text default null, p_err_msg_cn varchar default null,
    p_bgng_dt timestamp default null, p_end_dt timestamp default null, p_sort_no integer default null
)
language plpgsql as $$
begin
    if p_op = 'C' then
        if p_mig_job_tbl_id is null or trim(p_mig_job_tbl_id) = '' then
            p_mig_job_tbl_id := fn_next_etl_id('migt');
        end if;
        insert into t_mig_job_tbl (
            mig_job_tbl_id, mig_job_id, tbl_nm, tbl_stts_cd, row_cnt, batch_cnt,
            crt_tbl_ddl_cn, err_msg_cn, bgng_dt, end_dt, sort_no, crt_dt, mdfcn_dt
        ) values (
            p_mig_job_tbl_id, p_mig_job_id, p_tbl_nm, coalesce(p_tbl_stts_cd, 'PENDING'),
            p_row_cnt, p_batch_cnt, p_crt_tbl_ddl_cn, p_err_msg_cn,
            p_bgng_dt, p_end_dt, p_sort_no, current_timestamp, current_timestamp
        );
    elsif p_op = 'U' then
        update t_mig_job_tbl set
            tbl_stts_cd = coalesce(p_tbl_stts_cd, tbl_stts_cd), row_cnt = coalesce(p_row_cnt, row_cnt),
            batch_cnt = coalesce(p_batch_cnt, batch_cnt), crt_tbl_ddl_cn = coalesce(p_crt_tbl_ddl_cn, crt_tbl_ddl_cn),
            err_msg_cn = coalesce(p_err_msg_cn, err_msg_cn), bgng_dt = coalesce(p_bgng_dt, bgng_dt),
            end_dt = coalesce(p_end_dt, end_dt), mdfcn_dt = current_timestamp
        where mig_job_tbl_id = p_mig_job_tbl_id;
    elsif p_op = 'D' then
        delete from t_mig_job_tbl where mig_job_tbl_id = p_mig_job_tbl_id;
    else
        raise exception '지원하지 않는 Operation: %', p_op;
    end if;
end;
$$;

comment on function fn_next_etl_id(varchar) is '접두어별 일자 단위 순번을 증가시키고 {prefix}-YYYYMMDD-NNN 형식 ID를 반환한다';

comment on procedure sp_lnkg_profile(character, inout character varying, character varying, character varying, character varying, integer, character varying, character varying, character varying, character varying, text, boolean, character varying, character varying, timestamp without time zone, character varying) is '연결 프로필 CUD. op: C=등록(ID 미입력 시 lnkg 접두어 자동 채번), U=부분 수정, D=논리삭제(DELETED)';

comment on procedure sp_mig_job(character, inout character varying, character varying, character varying, character varying, character varying, character varying, integer, boolean, character varying, integer, integer, integer, bigint, character varying, timestamp without time zone, timestamp without time zone, character varying) is '마이그레이션 배치 작업 CUD. op: C=등록(etl 접두어 자동 채번), U=상태·건수 갱신, D=물리 삭제';

comment on procedure sp_mig_job_tbl(character, inout character varying, character varying, character varying, character varying, bigint, integer, text, character varying, timestamp without time zone, timestamp without time zone, integer) is '마이그레이션 테이블 작업 CUD. op: C=등록(migt 접두어 자동 채번), U=적재 결과 갱신, D=물리 삭제';
