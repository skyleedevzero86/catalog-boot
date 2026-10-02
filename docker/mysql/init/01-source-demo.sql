/*
 * ----------------------------------------------------------------------------
 * docker/mysql/init/01-source-demo.sql
 * 목적 : 로컬 MySQL 원천/타깃 샘플(employees). catalog/catalog @ source_db
 * 주의 : docker compose mysql 최초 기동 전용. 운영 적용 금지.
 * ----------------------------------------------------------------------------
 */

-- ---------------------------------------------------------------------------
-- employees : MySQL 원천/타깃 데모. ON DUPLICATE KEY 로 시드 재적용 허용.
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
