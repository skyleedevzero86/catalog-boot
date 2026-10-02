package com.sleekydz86.catalog.unit.jdbc;

import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcSqlDialect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JdbcSqlDialect Oracle DROP")
class JdbcSqlDialectOracleDropTest {

    @Test
    @DisplayName("PL/SQL 문자열 내 작은따옴표를 이스케이프한다")
    void escapesSingleQuotesInOracleDrop() {
        String sql = JdbcSqlDialect.oracleDropTablePlSql("\"SCH\".\"T'BL\"");
        assertThat(sql).isEqualTo(
                "BEGIN EXECUTE IMMEDIATE 'DROP TABLE \"SCH\".\"T''BL\"'; EXCEPTION WHEN OTHERS THEN NULL; END;"
        );
    }
}
