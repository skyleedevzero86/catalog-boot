create or replace procedure sp_extr_dmnd(
    p_op char(1),
    inout p_extr_dmnd_id varchar,
    p_dmnd_src_cd varchar default 'EXTERNAL_API',
    p_extr_dmnd_nm varchar default null,
    p_extr_dmnd_stts_cd varchar default 'READY',
    p_datst_cnt integer default 1,
    p_actor_id varchar default 'system'
)
language plpgsql as $$
begin
    if p_op = 'C' then
        if p_extr_dmnd_id is null or trim(p_extr_dmnd_id) = '' then
            p_extr_dmnd_id := fn_next_etl_id('xdm');
        end if;
        insert into t_extr_dmnd (
            extr_dmnd_id, dmnd_src_cd, extr_form_id, extr_dmnd_nm, extr_dmnd_stts_cd,
            otsd_dmnd_id, datst_cnt, creatr_id, mdfr_id, crt_dt, mdfcn_dt
        ) values (
            p_extr_dmnd_id,
            coalesce(p_dmnd_src_cd, 'EXTERNAL_API'),
            null,
            coalesce(p_extr_dmnd_nm, p_extr_dmnd_id),
            coalesce(p_extr_dmnd_stts_cd, 'READY'),
            null,
            coalesce(p_datst_cnt, 1),
            p_actor_id, p_actor_id, current_timestamp, current_timestamp
        );
    elsif p_op = 'U' then
        update t_extr_dmnd set
            extr_dmnd_stts_cd = coalesce(p_extr_dmnd_stts_cd, extr_dmnd_stts_cd),
            mdfr_id = p_actor_id,
            mdfcn_dt = current_timestamp
        where extr_dmnd_id = p_extr_dmnd_id;
    elsif p_op = 'D' then
        delete from t_extr_dmnd where extr_dmnd_id = p_extr_dmnd_id;
    end if;
end;
$$;

create or replace procedure sp_extr_datst(
    p_op char(1),
    inout p_extr_datst_id varchar,
    p_extr_dmnd_id varchar default null,
    p_extr_spcf_id varchar default null,
    p_datst_nm varchar default null,
    p_extr_datst_stts_cd varchar default 'READY',
    p_sort_no integer default 1,
    p_mnfst_cn text default null,
    p_actor_id varchar default 'system'
)
language plpgsql as $$
begin
    if p_op = 'C' then
        if p_extr_datst_id is null or trim(p_extr_datst_id) = '' then
            p_extr_datst_id := fn_next_etl_id('datst');
        end if;
        insert into t_extr_datst (
            extr_datst_id, extr_dmnd_id, extr_spcf_id, datst_nm, extr_datst_stts_cd,
            sort_no, mnfst_cn, creatr_id, mdfr_id, crt_dt, mdfcn_dt
        ) values (
            p_extr_datst_id,
            p_extr_dmnd_id,
            p_extr_spcf_id,
            coalesce(p_datst_nm, p_extr_datst_id),
            coalesce(p_extr_datst_stts_cd, 'READY'),
            coalesce(p_sort_no, 1),
            p_mnfst_cn,
            p_actor_id, p_actor_id, current_timestamp, current_timestamp
        );
    elsif p_op = 'U' then
        update t_extr_datst set
            extr_datst_stts_cd = coalesce(p_extr_datst_stts_cd, extr_datst_stts_cd),
            mnfst_cn = coalesce(p_mnfst_cn, mnfst_cn),
            mdfr_id = p_actor_id,
            mdfcn_dt = current_timestamp
        where extr_datst_id = p_extr_datst_id;
    elsif p_op = 'D' then
        delete from t_extr_datst where extr_datst_id = p_extr_datst_id;
    end if;
end;
$$;

create or replace procedure sp_extr_excn(
    p_op char(1),
    inout p_extr_excn_id varchar,
    p_extr_datst_id varchar,
    p_excn_type_cd varchar default null,
    p_excn_stts_cd varchar default 'RUNNING',
    p_rslt_nocs bigint default null,
    p_rslt_strg_type_cd varchar default null,
    p_rslt_tbl_nm varchar default null,
    p_rslt_file_path text default null,
    p_fail_cn varchar default null,
    p_excn_req_cn text default null,
    p_actor_id varchar default 'system'
)
language plpgsql as $$
declare
    v_excn_sn integer;
begin
    if p_op = 'C' then
        if p_extr_excn_id is null or trim(p_extr_excn_id) = '' then
            p_extr_excn_id := fn_next_etl_id('xex');
        end if;
        select coalesce(max(excn_sn), 0) + 1
        into v_excn_sn
        from t_extr_excn
        where extr_datst_id = p_extr_datst_id;

        insert into t_extr_excn (
            extr_excn_id, extr_datst_id, excn_sn, excn_type_cd, excn_stts_cd,
            extr_job_id, excn_bgng_dt, excn_end_dt, rslt_nocs, rslt_strg_type_cd,
            rslt_tbl_nm, rslt_file_path, fail_cn, checkpoint_state, resumed_from_checkpoint,
            resume_message, excn_sql_cn, excn_sql_vrbl_cn, excn_req_cn,
            creatr_id, mdfr_id, crt_dt, mdfcn_dt
        ) values (
            p_extr_excn_id, p_extr_datst_id, v_excn_sn,
            p_excn_type_cd, coalesce(p_excn_stts_cd, 'RUNNING'),
            null, current_timestamp,
            case when p_excn_stts_cd in ('SUCCESS', 'FAILED', 'CANCELLED') then current_timestamp else null end,
            p_rslt_nocs, p_rslt_strg_type_cd, p_rslt_tbl_nm, p_rslt_file_path, p_fail_cn,
            null, false, null, null, null, p_excn_req_cn,
            p_actor_id, p_actor_id, current_timestamp, current_timestamp
        );
    elsif p_op = 'D' then
        delete from t_extr_excn where extr_excn_id = p_extr_excn_id;
    end if;
end;
$$;

comment on procedure sp_extr_dmnd is '추출 요청 CUD. op: C=등록(xdm 접두어), U=상태 갱신, D=삭제';
comment on procedure sp_extr_datst is '추출 데이터셋 CUD. op: C=등록, U=상태·매니페스트 갱신, D=삭제';
comment on procedure sp_extr_excn is '추출 실행 이력 등록. op: C=등록(xex 접두어, excn_sn 자동), D=삭제';
