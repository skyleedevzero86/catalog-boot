/*
 * ============================================================================
 * V6__job_cancelled_status.sql
 * ----------------------------------------------------------------------------
 * 목적   : 마이그레이션 job 상태값에 CANCELLED 추가
 * 대상   : PostgreSQL / etl_data.t_mig_job
 * 의존   : V2
 * 배경   : 비동기 배치 취소 API 지원. CHECK 제약은 drop 후 재생성.
 * 롤백   : CANCELLED 행이 없을 때만 이전 CHECK 로 복구 가능
 * ============================================================================
 */

-- CHECK 재생성: 기존 제약에 CANCELLED 를 추가한다.
-- 실행 중 취소 API 가 PENDING/RUNNING → CANCELLED 전이를 허용하기 위함.
alter table t_mig_job drop constraint if exists ck_t_mig_job_stts;
alter table t_mig_job add constraint ck_t_mig_job_stts
    check (job_stts_cd in ('PENDING', 'RUNNING', 'SUCCESS', 'PARTIAL_SUCCESS', 'FAILED', 'CANCELLED'));
