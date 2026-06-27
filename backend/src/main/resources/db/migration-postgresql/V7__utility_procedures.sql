create or replace procedure sp_mig_job_tbl_reset_failed(p_mig_job_id varchar)
language plpgsql as $$
begin
    update t_mig_job_tbl set
        tbl_stts_cd = 'PENDING',
        row_cnt = null,
        batch_cnt = null,
        crt_tbl_ddl_cn = null,
        err_msg_cn = null,
        bgng_dt = null,
        end_dt = null,
        mdfcn_dt = current_timestamp
    where mig_job_id = p_mig_job_id
      and tbl_stts_cd = 'FAILED';
end;
$$;

create or replace procedure sp_mtdt_tbl_ctgr_mpng_clear(p_mtdt_tbl_ctgr_id varchar)
language plpgsql as $$
begin
    delete from t_mtdt_tbl_ctgr_mpng where mtdt_tbl_ctgr_id = p_mtdt_tbl_ctgr_id;
end;
$$;

comment on procedure sp_mig_job_tbl_reset_failed(character varying) is '마이그레이션 작업의 FAILED 테이블 행을 PENDING으로 초기화한다';

comment on procedure sp_mtdt_tbl_ctgr_mpng_clear(character varying) is '카테고리에 매핑된 메타테이블 매핑을 전부 삭제한다';
