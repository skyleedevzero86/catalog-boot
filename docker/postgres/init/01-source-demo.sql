/*
 * ----------------------------------------------------------------------------
 * docker/postgres/init/01-source-demo.sql
 * 목적 : 로컬 데모용 원천 스키마(demo.employees). Control 스키마(etl_data)와 분리.
 * 주의 : 컨테이너 최초 초기 시에만 실행된다. 운영 DB 에 적용하지 말 것.
 * ----------------------------------------------------------------------------
 */

-- ---------------------------------------------------------------------------
-- demo.employees : 마이그레이션/추출 연동 검증용 샘플 원천 테이블
-- Control Plane(etl_data) 과 스키마를 분리해 원천 JDBC 경로만 검증한다.
-- ON CONFLICT : 컨테이너 재초기화 없이 재실행해도 시드가 깨지지 않게 한다.
-- ---------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS demo;

CREATE TABLE IF NOT EXISTS demo.employees (
    emp_id      INTEGER PRIMARY KEY,
    emp_name    VARCHAR(100) NOT NULL,
    dept_code   VARCHAR(20),
    hired_at    DATE,
    salary      NUMERIC(12, 2)
);

INSERT INTO demo.employees (emp_id, emp_name, dept_code, hired_at, salary)
VALUES
    (1, 'Kim', 'HR', DATE '2024-01-10', 4200.00),
    (2, 'Lee', 'IT', DATE '2023-06-01', 5100.50),
    (3, 'Park', 'FIN', DATE '2025-03-15', 3900.00)
ON CONFLICT (emp_id) DO NOTHING;
