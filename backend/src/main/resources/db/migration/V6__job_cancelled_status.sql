alter table t_mig_job drop constraint if exists ck_t_mig_job_stts;
alter table t_mig_job add constraint ck_t_mig_job_stts
    check (job_stts_cd in ('PENDING', 'RUNNING', 'SUCCESS', 'PARTIAL_SUCCESS', 'FAILED', 'CANCELLED'));
