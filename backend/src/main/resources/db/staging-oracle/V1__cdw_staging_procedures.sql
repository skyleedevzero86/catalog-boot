/*
 * ============================================================================
 * V1__cdw_staging_procedures.sql (Oracle 12c+)
 * ----------------------------------------------------------------------------
 * 목적   : 파일 적재용 CREATE/DROP·행 CUD를 JDBC INSERT가 아닌 sp_* 로 수행
 * 대상   : 등록 연결의 Oracle (운영자가 대상 스키마에 수동 설치)
 * 의존   : JSON_OBJECT_T / JSON_ARRAY_T (Oracle Database 12.2+)
 * 계약   : sp_cdw_stg_tbl  p_op = 'C'|'D'
 *          sp_cdw_stg_rows p_op = 'C'|'U'|'D' , p_payload JSON 텍스트
 * 주의   : Control Flyway 대상 아님. 식별자는 영문·숫자·_ 만 허용.
 * ============================================================================
 */

CREATE OR REPLACE PROCEDURE sp_cdw_stg_tbl(
    p_op          IN CHAR,
    p_schema      IN VARCHAR2,
    p_table       IN VARCHAR2,
    p_columns_ddl IN CLOB DEFAULT NULL
) AS
    v_sql CLOB;
BEGIN
    IF p_schema IS NULL OR NOT REGEXP_LIKE(p_schema, '^[A-Za-z_][A-Za-z0-9_]*$') THEN
        RAISE_APPLICATION_ERROR(-20001, 'INVALID_SCHEMA');
    END IF;
    IF p_table IS NULL OR NOT REGEXP_LIKE(p_table, '^[A-Za-z_][A-Za-z0-9_]*$') THEN
        RAISE_APPLICATION_ERROR(-20002, 'INVALID_TABLE');
    END IF;

    IF p_op = 'D' THEN
        BEGIN
            EXECUTE IMMEDIATE 'DROP TABLE ' || p_schema || '.' || p_table || ' PURGE';
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE != -942 THEN
                    RAISE;
                END IF;
        END;
    ELSIF p_op = 'C' THEN
        IF p_columns_ddl IS NULL OR LENGTH(TRIM(p_columns_ddl)) = 0 THEN
            RAISE_APPLICATION_ERROR(-20003, 'COLUMNS_DDL_REQUIRED');
        END IF;
        v_sql := 'CREATE TABLE ' || p_schema || '.' || p_table || ' (' || p_columns_ddl || ')';
        EXECUTE IMMEDIATE v_sql;
    ELSE
        RAISE_APPLICATION_ERROR(-20004, 'UNSUPPORTED_OP');
    END IF;
END;
/

CREATE OR REPLACE PROCEDURE sp_cdw_stg_rows(
    p_op      IN CHAR,
    p_schema  IN VARCHAR2,
    p_table   IN VARCHAR2,
    p_payload IN CLOB
) AS
    v_root      JSON_OBJECT_T;
    v_cols      JSON_ARRAY_T;
    v_rows      JSON_ARRAY_T;
    v_row       JSON_ARRAY_T;
    v_col_list  VARCHAR2(4000);
    v_val_list  CLOB;
    v_set_list  CLOB;
    v_keys      CLOB;
    v_col       VARCHAR2(128);
    v_key_col   VARCHAR2(128);
    v_cell      VARCHAR2(4000);
    v_sql       CLOB;
    v_i         PLS_INTEGER;
    v_j         PLS_INTEGER;
BEGIN
    IF p_schema IS NULL OR NOT REGEXP_LIKE(p_schema, '^[A-Za-z_][A-Za-z0-9_]*$')
        OR p_table IS NULL OR NOT REGEXP_LIKE(p_table, '^[A-Za-z_][A-Za-z0-9_]*$') THEN
        RAISE_APPLICATION_ERROR(-20001, 'INVALID_IDENTIFIER');
    END IF;
    IF p_payload IS NULL OR LENGTH(TRIM(p_payload)) = 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'PAYLOAD_REQUIRED');
    END IF;

    v_root := JSON_OBJECT_T.parse(p_payload);
    v_cols := TREAT(v_root.get('columns') AS JSON_ARRAY_T);
    v_rows := TREAT(v_root.get('rows') AS JSON_ARRAY_T);
    IF v_cols IS NULL OR v_cols.get_size() = 0 THEN
        RAISE_APPLICATION_ERROR(-20006, 'COLUMNS_REQUIRED');
    END IF;

    v_col_list := NULL;
    FOR v_i IN 0 .. v_cols.get_size() - 1 LOOP
        v_col := v_cols.get_string(v_i);
        IF v_col IS NULL OR NOT REGEXP_LIKE(v_col, '^[A-Za-z_][A-Za-z0-9_]*$') THEN
            RAISE_APPLICATION_ERROR(-20007, 'INVALID_COLUMN');
        END IF;
        IF v_col_list IS NULL THEN
            v_col_list := '"' || v_col || '"';
        ELSE
            v_col_list := v_col_list || ', "' || v_col || '"';
        END IF;
    END LOOP;
    v_key_col := v_cols.get_string(0);

    IF p_op = 'C' THEN
        IF v_rows IS NULL THEN
            RETURN;
        END IF;
        FOR v_i IN 0 .. v_rows.get_size() - 1 LOOP
            v_row := TREAT(v_rows.get(v_i) AS JSON_ARRAY_T);
            IF v_row IS NULL OR v_row.get_size() <> v_cols.get_size() THEN
                RAISE_APPLICATION_ERROR(-20008, 'ROW_WIDTH_MISMATCH');
            END IF;
            v_val_list := NULL;
            FOR v_j IN 0 .. v_row.get_size() - 1 LOOP
                IF v_val_list IS NOT NULL THEN
                    v_val_list := v_val_list || ', ';
                ELSE
                    v_val_list := '';
                END IF;
                IF v_row.get(v_j) IS NULL OR v_row.get(v_j).is_null THEN
                    v_val_list := v_val_list || 'NULL';
                ELSE
                    v_cell := v_row.get_string(v_j);
                    v_val_list := v_val_list || '''' || REPLACE(v_cell, '''', '''''') || '''';
                END IF;
            END LOOP;
            v_sql := 'INSERT INTO ' || p_schema || '.' || p_table
                || ' (' || v_col_list || ') VALUES (' || v_val_list || ')';
            EXECUTE IMMEDIATE v_sql;
        END LOOP;

    ELSIF p_op = 'U' THEN
        IF v_rows IS NULL THEN
            RETURN;
        END IF;
        FOR v_i IN 0 .. v_rows.get_size() - 1 LOOP
            v_row := TREAT(v_rows.get(v_i) AS JSON_ARRAY_T);
            IF v_row IS NULL OR v_row.get_size() <> v_cols.get_size() THEN
                RAISE_APPLICATION_ERROR(-20008, 'ROW_WIDTH_MISMATCH');
            END IF;
            IF v_row.get(0) IS NULL OR v_row.get(0).is_null THEN
                RAISE_APPLICATION_ERROR(-20009, 'UPDATE_KEY_NULL');
            END IF;
            v_set_list := NULL;
            FOR v_j IN 1 .. v_cols.get_size() - 1 LOOP
                v_col := v_cols.get_string(v_j);
                IF v_set_list IS NOT NULL THEN
                    v_set_list := v_set_list || ', ';
                ELSE
                    v_set_list := '';
                END IF;
                IF v_row.get(v_j) IS NULL OR v_row.get(v_j).is_null THEN
                    v_set_list := v_set_list || '"' || v_col || '"=NULL';
                ELSE
                    v_cell := v_row.get_string(v_j);
                    v_set_list := v_set_list || '"' || v_col || '"='''
                        || REPLACE(v_cell, '''', '''''') || '''';
                END IF;
            END LOOP;
            v_cell := v_row.get_string(0);
            v_sql := 'UPDATE ' || p_schema || '.' || p_table || ' SET ' || v_set_list
                || ' WHERE "' || v_key_col || '"=''' || REPLACE(v_cell, '''', '''''') || '''';
            EXECUTE IMMEDIATE v_sql;
        END LOOP;

    ELSIF p_op = 'D' THEN
        IF v_rows IS NULL OR v_rows.get_size() = 0 THEN
            RETURN;
        END IF;
        v_keys := NULL;
        FOR v_i IN 0 .. v_rows.get_size() - 1 LOOP
            IF v_rows.get(v_i).is_array THEN
                v_row := TREAT(v_rows.get(v_i) AS JSON_ARRAY_T);
                IF v_row.get(0) IS NULL OR v_row.get(0).is_null THEN
                    RAISE_APPLICATION_ERROR(-20010, 'DELETE_KEY_NULL');
                END IF;
                v_cell := v_row.get_string(0);
            ELSE
                IF v_rows.get(v_i) IS NULL OR v_rows.get(v_i).is_null THEN
                    RAISE_APPLICATION_ERROR(-20010, 'DELETE_KEY_NULL');
                END IF;
                v_cell := v_rows.get_string(v_i);
            END IF;
            IF v_keys IS NULL THEN
                v_keys := '''' || REPLACE(v_cell, '''', '''''') || '''';
            ELSE
                v_keys := v_keys || ',''' || REPLACE(v_cell, '''', '''''') || '''';
            END IF;
        END LOOP;
        v_sql := 'DELETE FROM ' || p_schema || '.' || p_table
            || ' WHERE "' || v_key_col || '" IN (' || v_keys || ')';
        EXECUTE IMMEDIATE v_sql;

    ELSE
        RAISE_APPLICATION_ERROR(-20004, 'UNSUPPORTED_OP');
    END IF;
END;
/
