/*
 * ----------------------------------------------------------------------------
 * docker/mariadb/init/01-source-demo.sql
 * 목적 : 로컬 MariaDB 샘플(employees). 호스트 포트 3307 → 컨테이너 3306
 * 주의 : docker compose mariadb 최초 기동 전용. 운영 적용 금지.
 * ----------------------------------------------------------------------------
 */

-- ---------------------------------------------------------------------------
-- employees : MariaDB 원천 데모. MySQL 스크립트와 동일 시드 계약.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employees (
    emp_id      INT PRIMARY KEY,
    emp_name    VARCHAR(100) NOT NULL,
    dept_code   VARCHAR(20),
    hired_at    DATE,
    salary      DECIMAL(12, 2)
);

INSERT INTO employees (emp_id, emp_name, dept_code, hired_at, salary)
VALUES
    (1, 'Kim', 'HR', '2024-01-10', 4200.00),
    (2, 'Lee', 'IT', '2023-06-01', 5100.50),
    (3, 'Park', 'FIN', '2025-03-15', 3900.00)
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);
