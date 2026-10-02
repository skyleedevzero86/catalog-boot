/*
 * ============================================================================
 * V1__cdw_staging_procedures.sql (MySQL 8+ / MariaDB 10.6+)
 * ----------------------------------------------------------------------------
 * 목적   : 파일 적재·스테이징용 CREATE/DROP·행 CUD를 JDBC INSERT가 아닌 sp_* 로 수행
 * 대상   : 등록 연결의 MySQL 또는 MariaDB (운영자가 대상 DB에 수동 설치)
 * 의존   : JSON 함수 (JSON_LENGTH / JSON_EXTRACT / JSON_UNQUOTE)
 * 계약   : sp_cdw_stg_tbl  p_op = 'C'|'D'
 *          sp_cdw_stg_rows p_op = 'C'|'U'|'D' , p_payload JSON
 *            { "columns":["c1",...], "rows":[[...],...] }
 * 주의   : Control Flyway 대상 아님. 식별자는 영문·숫자·_ 만 허용.
 * ============================================================================
 */

DROP PROCEDURE IF EXISTS sp_cdw_stg_tbl;
DROP PROCEDURE IF EXISTS sp_cdw_stg_rows;

DELIMITER $$

-- ---------------------------------------------------------------------------
-- sp_cdw_stg_tbl : 테이블 생성(C) / 삭제(D)
-- ---------------------------------------------------------------------------
CREATE PROCEDURE sp_cdw_stg_tbl(
    IN p_op CHAR(1),
    IN p_schema VARCHAR(64),
    IN p_table VARCHAR(64),
    IN p_columns_ddl MEDIUMTEXT
)
BEGIN
    DECLARE v_sql MEDIUMTEXT;

    IF p_schema IS NULL OR p_schema NOT REGEXP '^[A-Za-z_][A-Za-z0-9_]*$' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_SCHEMA';
    END IF;
    IF p_table IS NULL OR p_table NOT REGEXP '^[A-Za-z_][A-Za-z0-9_]*$' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_TABLE';
    END IF;

    IF p_op = 'D' THEN
        SET v_sql = CONCAT('DROP TABLE IF EXISTS `', p_schema, '`.`', p_table, '`');
        SET @cdw_stg_sql = v_sql;
        PREPARE cdw_stg_stmt FROM @cdw_stg_sql;
        EXECUTE cdw_stg_stmt;
        DEALLOCATE PREPARE cdw_stg_stmt;
    ELSEIF p_op = 'C' THEN
        IF p_columns_ddl IS NULL OR TRIM(p_columns_ddl) = '' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'COLUMNS_DDL_REQUIRED';
        END IF;
        SET v_sql = CONCAT('CREATE TABLE `', p_schema, '`.`', p_table, '` (', p_columns_ddl, ')');
        SET @cdw_stg_sql = v_sql;
        PREPARE cdw_stg_stmt FROM @cdw_stg_sql;
        EXECUTE cdw_stg_stmt;
        DEALLOCATE PREPARE cdw_stg_stmt;
    ELSE
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'UNSUPPORTED_OP';
    END IF;
END$$

-- ---------------------------------------------------------------------------
-- sp_cdw_stg_rows : 행 CUD (C=Insert, U=Update 첫컬럼=키, D=Delete)
-- ---------------------------------------------------------------------------
CREATE PROCEDURE sp_cdw_stg_rows(
    IN p_op CHAR(1),
    IN p_schema VARCHAR(64),
    IN p_table VARCHAR(64),
    IN p_payload LONGTEXT
)
proc_body: BEGIN
    DECLARE v_col_count INT DEFAULT 0;
    DECLARE v_row_count INT DEFAULT 0;
    DECLARE v_i INT DEFAULT 0;
    DECLARE v_j INT DEFAULT 0;
    DECLARE v_col_list TEXT DEFAULT '';
    DECLARE v_val_list TEXT DEFAULT '';
    DECLARE v_set_list TEXT DEFAULT '';
    DECLARE v_keys TEXT DEFAULT '';
    DECLARE v_col VARCHAR(64);
    DECLARE v_key_col VARCHAR(64);
    DECLARE v_cell_type VARCHAR(32);
    DECLARE v_cell TEXT;
    DECLARE v_sql MEDIUMTEXT;

    IF p_schema IS NULL OR p_schema NOT REGEXP '^[A-Za-z_][A-Za-z0-9_]*$'
        OR p_table IS NULL OR p_table NOT REGEXP '^[A-Za-z_][A-Za-z0-9_]*$' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_IDENTIFIER';
    END IF;
    IF p_payload IS NULL OR TRIM(p_payload) = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'PAYLOAD_REQUIRED';
    END IF;

    SET v_col_count = JSON_LENGTH(p_payload, '$.columns');
    IF v_col_count IS NULL OR v_col_count < 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'COLUMNS_REQUIRED';
    END IF;

    SET v_i = 0;
    WHILE v_i < v_col_count DO
        SET v_col = JSON_UNQUOTE(JSON_EXTRACT(p_payload, CONCAT('$.columns[', v_i, ']')));
        IF v_col IS NULL OR v_col NOT REGEXP '^[A-Za-z_][A-Za-z0-9_]*$' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_COLUMN';
        END IF;
        IF v_i > 0 THEN
            SET v_col_list = CONCAT(v_col_list, ', ');
        END IF;
        SET v_col_list = CONCAT(v_col_list, '`', v_col, '`');
        SET v_i = v_i + 1;
    END WHILE;

    SET v_key_col = JSON_UNQUOTE(JSON_EXTRACT(p_payload, '$.columns[0]'));
    SET v_row_count = IFNULL(JSON_LENGTH(p_payload, '$.rows'), 0);

    IF p_op = 'C' THEN
        SET v_i = 0;
        WHILE v_i < v_row_count DO
            IF JSON_LENGTH(p_payload, CONCAT('$.rows[', v_i, ']')) <> v_col_count THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ROW_WIDTH_MISMATCH';
            END IF;
            SET v_val_list = '';
            SET v_j = 0;
            WHILE v_j < v_col_count DO
                IF v_j > 0 THEN
                    SET v_val_list = CONCAT(v_val_list, ', ');
                END IF;
                SET v_cell_type = JSON_TYPE(JSON_EXTRACT(p_payload, CONCAT('$.rows[', v_i, '][', v_j, ']')));
                IF v_cell_type = 'NULL' THEN
                    SET v_val_list = CONCAT(v_val_list, 'NULL');
                ELSE
                    SET v_cell = JSON_UNQUOTE(JSON_EXTRACT(p_payload, CONCAT('$.rows[', v_i, '][', v_j, ']')));
                    SET v_val_list = CONCAT(v_val_list, QUOTE(v_cell));
                END IF;
                SET v_j = v_j + 1;
            END WHILE;
            SET v_sql = CONCAT(
                'INSERT INTO `', p_schema, '`.`', p_table, '` (', v_col_list, ') VALUES (', v_val_list, ')'
            );
            SET @cdw_stg_sql = v_sql;
            PREPARE cdw_stg_stmt FROM @cdw_stg_sql;
            EXECUTE cdw_stg_stmt;
            DEALLOCATE PREPARE cdw_stg_stmt;
            SET v_i = v_i + 1;
        END WHILE;

    ELSEIF p_op = 'U' THEN
        SET v_i = 0;
        WHILE v_i < v_row_count DO
            IF JSON_LENGTH(p_payload, CONCAT('$.rows[', v_i, ']')) <> v_col_count THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ROW_WIDTH_MISMATCH';
            END IF;
            IF JSON_TYPE(JSON_EXTRACT(p_payload, CONCAT('$.rows[', v_i, '][0]'))) = 'NULL' THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'UPDATE_KEY_NULL';
            END IF;
            SET v_set_list = '';
            SET v_j = 1;
            WHILE v_j < v_col_count DO
                SET v_col = JSON_UNQUOTE(JSON_EXTRACT(p_payload, CONCAT('$.columns[', v_j, ']')));
                IF v_j > 1 THEN
                    SET v_set_list = CONCAT(v_set_list, ', ');
                END IF;
                SET v_cell_type = JSON_TYPE(JSON_EXTRACT(p_payload, CONCAT('$.rows[', v_i, '][', v_j, ']')));
                IF v_cell_type = 'NULL' THEN
                    SET v_set_list = CONCAT(v_set_list, '`', v_col, '`=NULL');
                ELSE
                    SET v_cell = JSON_UNQUOTE(JSON_EXTRACT(p_payload, CONCAT('$.rows[', v_i, '][', v_j, ']')));
                    SET v_set_list = CONCAT(v_set_list, '`', v_col, '`=', QUOTE(v_cell));
                END IF;
                SET v_j = v_j + 1;
            END WHILE;
            SET v_cell = JSON_UNQUOTE(JSON_EXTRACT(p_payload, CONCAT('$.rows[', v_i, '][0]')));
            SET v_sql = CONCAT(
                'UPDATE `', p_schema, '`.`', p_table, '` SET ', v_set_list,
                ' WHERE `', v_key_col, '`=', QUOTE(v_cell)
            );
            SET @cdw_stg_sql = v_sql;
            PREPARE cdw_stg_stmt FROM @cdw_stg_sql;
            EXECUTE cdw_stg_stmt;
            DEALLOCATE PREPARE cdw_stg_stmt;
            SET v_i = v_i + 1;
        END WHILE;

    ELSEIF p_op = 'D' THEN
        IF v_row_count = 0 THEN
            LEAVE proc_body;
        END IF;
        SET v_keys = '';
        SET v_i = 0;
        WHILE v_i < v_row_count DO
            IF v_i > 0 THEN
                SET v_keys = CONCAT(v_keys, ', ');
            END IF;
            IF JSON_TYPE(JSON_EXTRACT(p_payload, CONCAT('$.rows[', v_i, ']'))) = 'ARRAY' THEN
                IF JSON_TYPE(JSON_EXTRACT(p_payload, CONCAT('$.rows[', v_i, '][0]'))) = 'NULL' THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DELETE_KEY_NULL';
                END IF;
                SET v_cell = JSON_UNQUOTE(JSON_EXTRACT(p_payload, CONCAT('$.rows[', v_i, '][0]')));
            ELSE
                IF JSON_TYPE(JSON_EXTRACT(p_payload, CONCAT('$.rows[', v_i, ']'))) = 'NULL' THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DELETE_KEY_NULL';
                END IF;
                SET v_cell = JSON_UNQUOTE(JSON_EXTRACT(p_payload, CONCAT('$.rows[', v_i, ']')));
            END IF;
            SET v_keys = CONCAT(v_keys, QUOTE(v_cell));
            SET v_i = v_i + 1;
        END WHILE;
        SET v_sql = CONCAT(
            'DELETE FROM `', p_schema, '`.`', p_table, '` WHERE `', v_key_col, '` IN (', v_keys, ')'
        );
        SET @cdw_stg_sql = v_sql;
        PREPARE cdw_stg_stmt FROM @cdw_stg_sql;
        EXECUTE cdw_stg_stmt;
        DEALLOCATE PREPARE cdw_stg_stmt;

    ELSE
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'UNSUPPORTED_OP';
    END IF;
END$$

DELIMITER ;
