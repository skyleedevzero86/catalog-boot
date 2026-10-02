/*
 * ----------------------------------------------------------------------------
 * docker/clickhouse/init/01-source-demo.sql
 * 목적 : 로컬 ClickHouse 샘플(source_db.employees, MergeTree)
 * 주의 : clickhouse-init 컨테이너가 1회 실행. 재실행 시 중복 INSERT 가능.
 * ----------------------------------------------------------------------------
 */

-- ---------------------------------------------------------------------------
-- source_db.employees : ClickHouse MergeTree 샘플.
-- ORDER BY emp_id 만 지정(파티션 없음). 데모 규모에서 충분하다.
-- 재실행 시 INSERT 가 누적될 수 있으므로 초기화 컨테이너 1회 전제를 지킨다.
-- ---------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS source_db;

CREATE TABLE IF NOT EXISTS source_db.employees
(
    emp_id    Int32,
    emp_name  String,
    dept_code Nullable(String),
    hired_at  Nullable(Date),
    salary    Nullable(Decimal(12, 2))
)
ENGINE = MergeTree
ORDER BY emp_id;

INSERT INTO source_db.employees (emp_id, emp_name, dept_code, hired_at, salary)
VALUES
    (1, 'Kim', 'HR', '2024-01-10', 4200.00),
    (2, 'Lee', 'IT', '2023-06-01', 5100.50),
    (3, 'Park', 'FIN', '2025-03-15', 3900.00);
