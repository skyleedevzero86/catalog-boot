/*
 * ============================================================================
 * V1__cdw_staging_procedures.sql (PostgreSQL 스테이징 DB에 수동/별도 설치)
 * ----------------------------------------------------------------------------
 * 목적   : Extract 스테이징 테이블 CUD·중복제거를 Control JDBC DDL이 아닌 sp_* 로 수행
 * 대상   : Extract staging 으로 쓰는 PostgreSQL (원천/타깃이 아님)
 * 의존   : 없음 (스테이징 DB 스키마에 단독 설치)
 * 계약   : sp_cdw_stg_tbl p_op = 'C'|'D'
 *          - C: p_columns_ddl 로 CREATE TABLE (본문만, 괄호·스키마는 프로시저가 조립)
 *          - D: DROP TABLE IF EXISTS … CASCADE (삭제는 프로시저 내부)
 * 주의   : Control DB Flyway 가 아님. 스테이징 연결 DB에 DBA/운영이 적용.
 *          식별자는 영문·숫자·_ 만 허용 (SQL injection 방지).
 * ============================================================================
 */

-- ---------------------------------------------------------------------------
-- sp_cdw_stg_tbl : 스테이징 테이블 생성(C) / 삭제(D)
-- D 시 비즈니스 분기 없음. 존재하면 DROP IF EXISTS CASCADE 만 수행.
-- ---------------------------------------------------------------------------
create or replace procedure sp_cdw_stg_tbl(
    p_op char(1),
    p_schema varchar,
    p_table varchar,
    p_columns_ddl text default null
)
language plpgsql
as $$
declare
    v_schema text;
    v_table text;
    v_sql text;
begin
    if p_schema is null or trim(p_schema) = '' or p_schema !~ '^[A-Za-z_][A-Za-z0-9_]*$' then
        raise exception 'INVALID_SCHEMA:%', coalesce(p_schema, '');
    end if;
    if p_table is null or trim(p_table) = '' or p_table !~ '^[A-Za-z_][A-Za-z0-9_]*$' then
        raise exception 'INVALID_TABLE:%', coalesce(p_table, '');
    end if;

    v_schema := p_schema;
    v_table := p_table;

    if p_op = 'D' then
        v_sql := format('DROP TABLE IF EXISTS %I.%I CASCADE', v_schema, v_table);
        execute v_sql;
    elsif p_op = 'C' then
        if p_columns_ddl is null or trim(p_columns_ddl) = '' then
            raise exception 'COLUMNS_DDL_REQUIRED';
        end if;
        v_sql := format('CREATE TABLE %I.%I (%s)', v_schema, v_table, p_columns_ddl);
        execute v_sql;
    else
        raise exception 'UNSUPPORTED_OP:%', p_op;
    end if;
end;
$$;

comment on procedure sp_cdw_stg_tbl(char, varchar, varchar, text) is
    'Extract 스테이징 테이블 CUD. op: C=CREATE(본문 DDL), D=DROP IF EXISTS CASCADE';

-- ---------------------------------------------------------------------------
-- sp_cdw_stg_dedup : raw → final 해시 기준 중복 제거 테이블 생성
-- 결과 건수는 OUT 파라미터로 반환 (도메인 메시지/정책 없음)
-- ---------------------------------------------------------------------------
create or replace procedure sp_cdw_stg_dedup(
    p_schema varchar,
    p_raw_table varchar,
    p_final_table varchar,
    inout p_raw_count bigint,
    inout p_final_count bigint
)
language plpgsql
as $$
declare
    v_sql text;
begin
    if p_schema is null or p_schema !~ '^[A-Za-z_][A-Za-z0-9_]*$'
        or p_raw_table is null or p_raw_table !~ '^[A-Za-z_][A-Za-z0-9_]*$'
        or p_final_table is null or p_final_table !~ '^[A-Za-z_][A-Za-z0-9_]*$' then
        raise exception 'INVALID_IDENTIFIER';
    end if;

    execute format('SELECT COUNT(*) FROM %I.%I', p_schema, p_raw_table) into p_raw_count;

    v_sql := format(
        'CREATE TABLE %I.%I AS SELECT DISTINCT ON ("__row_hash") * FROM %I.%I ORDER BY "__row_hash", "__row_no"',
        p_schema, p_final_table, p_schema, p_raw_table
    );
    execute v_sql;

    execute format('SELECT COUNT(*) FROM %I.%I', p_schema, p_final_table) into p_final_count;
end;
$$;

comment on procedure sp_cdw_stg_dedup(varchar, varchar, varchar, bigint, bigint) is
    'Extract 스테이징 중복제거. raw 테이블에서 DISTINCT ON(__row_hash) 로 final 생성';

-- ---------------------------------------------------------------------------
-- sp_cdw_stg_rows : 행 단위 CUD (Insert=C, Update=U, Delete=D)
-- p_payload JSON 텍스트:
--   { "columns": ["__row_no","__row_hash","c0001",...],
--     "rows": [[1,"hash","v"], ...] }
-- D: rows 는 키 컬럼(columns[1]) 값만 [[k1],[k2],...]
-- 앱 Java 에서는 INSERT/UPDATE/DELETE 문자열을 직접 실행하지 않음.
-- ---------------------------------------------------------------------------
create or replace procedure sp_cdw_stg_rows(
    p_op char(1),
    p_schema varchar,
    p_table varchar,
    p_payload text
)
language plpgsql
as $$
declare
    v_payload jsonb;
    v_cols text[];
    v_col_list text;
    v_val_list text;
    v_set_list text;
    v_elem jsonb;
    v_i int;
    v_j int;
    v_key_col text;
    v_keys text;
    v_cell text;
begin
    if p_schema is null or p_schema !~ '^[A-Za-z_][A-Za-z0-9_]*$'
        or p_table is null or p_table !~ '^[A-Za-z_][A-Za-z0-9_]*$' then
        raise exception 'INVALID_IDENTIFIER';
    end if;
    if p_payload is null or trim(p_payload) = '' then
        raise exception 'PAYLOAD_REQUIRED';
    end if;

    v_payload := p_payload::jsonb;
    select array_agg(x order by ord)
    into v_cols
    from jsonb_array_elements_text(v_payload->'columns') with ordinality as t(x, ord);

    if v_cols is null or array_length(v_cols, 1) is null then
        raise exception 'COLUMNS_REQUIRED';
    end if;

    for v_i in 1..array_length(v_cols, 1) loop
        if v_cols[v_i] !~ '^[A-Za-z_][A-Za-z0-9_]*$' then
            raise exception 'INVALID_COLUMN:%', v_cols[v_i];
        end if;
    end loop;

    v_col_list := '';
    for v_i in 1..array_length(v_cols, 1) loop
        if v_i > 1 then
            v_col_list := v_col_list || ', ';
        end if;
        v_col_list := v_col_list || quote_ident(v_cols[v_i]);
    end loop;

    if p_op = 'C' then
        for v_elem in select value from jsonb_array_elements(v_payload->'rows')
        loop
            if jsonb_typeof(v_elem) <> 'array' then
                raise exception 'ROW_MUST_BE_ARRAY';
            end if;
            if jsonb_array_length(v_elem) <> array_length(v_cols, 1) then
                raise exception 'ROW_WIDTH_MISMATCH';
            end if;
            v_val_list := '';
            for v_j in 0..jsonb_array_length(v_elem) - 1 loop
                if v_j > 0 then
                    v_val_list := v_val_list || ', ';
                end if;
                if v_elem->v_j = 'null'::jsonb then
                    v_val_list := v_val_list || 'NULL';
                else
                    v_val_list := v_val_list || quote_literal(v_elem->>v_j);
                end if;
            end loop;
            execute format(
                'INSERT INTO %I.%I (%s) VALUES (%s)',
                p_schema, p_table, v_col_list, v_val_list
            );
        end loop;

    elsif p_op = 'U' then
        v_key_col := v_cols[1];
        for v_elem in select value from jsonb_array_elements(v_payload->'rows')
        loop
            if jsonb_typeof(v_elem) <> 'array'
                or jsonb_array_length(v_elem) <> array_length(v_cols, 1) then
                raise exception 'ROW_WIDTH_MISMATCH';
            end if;
            v_set_list := '';
            for v_i in 2..array_length(v_cols, 1) loop
                if v_i > 2 then
                    v_set_list := v_set_list || ', ';
                end if;
                if v_elem->(v_i - 1) = 'null'::jsonb then
                    v_cell := 'NULL';
                else
                    v_cell := quote_literal(v_elem->> (v_i - 1));
                end if;
                v_set_list := v_set_list || quote_ident(v_cols[v_i]) || ' = ' || v_cell;
            end loop;
            if v_elem->0 = 'null'::jsonb then
                raise exception 'UPDATE_KEY_NULL';
            end if;
            execute format(
                'UPDATE %I.%I SET %s WHERE %I = %s',
                p_schema, p_table, v_set_list, v_key_col, quote_literal(v_elem->>0)
            );
        end loop;

    elsif p_op = 'D' then
        v_key_col := v_cols[1];
        v_keys := '';
        v_i := 0;
        for v_elem in select value from jsonb_array_elements(v_payload->'rows')
        loop
            v_i := v_i + 1;
            if v_i > 1 then
                v_keys := v_keys || ', ';
            end if;
            if jsonb_typeof(v_elem) = 'array' then
                if v_elem->0 = 'null'::jsonb then
                    raise exception 'DELETE_KEY_NULL';
                end if;
                v_keys := v_keys || quote_literal(v_elem->>0);
            else
                if v_elem = 'null'::jsonb then
                    raise exception 'DELETE_KEY_NULL';
                end if;
                v_keys := v_keys || quote_literal(v_elem#>>'{}');
            end if;
        end loop;
        if v_i = 0 then
            return;
        end if;
        execute format(
            'DELETE FROM %I.%I WHERE %I IN (%s)',
            p_schema, p_table, v_key_col, v_keys
        );

    else
        raise exception 'UNSUPPORTED_OP:%', p_op;
    end if;
end;
$$;

comment on procedure sp_cdw_stg_rows(char, varchar, varchar, text) is
    'Extract 스테이징 행 CUD. op: C=Insert, U=Update(첫 컬럼=키), D=Delete(첫 컬럼 IN)';
