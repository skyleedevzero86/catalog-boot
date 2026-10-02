package com.sleekydz86.catalog.adapter.outbound.staging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcUrlFactory;
import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class StagingMybatisGateway {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SqlSessionFactory> factories = new ConcurrentHashMap<>();

    public StagingMybatisGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void requirePostgreSQL(DatabaseEndpoint endpoint) {
        if (endpoint.vendor() != DatabaseVendor.POSTGRESQL) {
            throw new IllegalArgumentException(
                    "PostgreSQL + MyBatis sp_cdw_stg_* 만 지원합니다. vendor=" + endpoint.vendor()
            );
        }
    }

    public void requireSpVendor(DatabaseEndpoint endpoint) {
        DatabaseVendor vendor = endpoint.vendor();
        if (vendor != DatabaseVendor.POSTGRESQL
                && vendor != DatabaseVendor.MYSQL
                && vendor != DatabaseVendor.MARIADB
                && vendor != DatabaseVendor.ORACLE) {
            throw new IllegalArgumentException(
                    "sp_cdw_stg_* 는 PostgreSQL/MySQL/MariaDB/Oracle 만 지원합니다. vendor=" + vendor
            );
        }
    }

    public boolean supportsStoredProcedures(DatabaseEndpoint endpoint) {
        DatabaseVendor vendor = endpoint.vendor();
        return vendor == DatabaseVendor.POSTGRESQL
                || vendor == DatabaseVendor.MYSQL
                || vendor == DatabaseVendor.MARIADB
                || vendor == DatabaseVendor.ORACLE;
    }

    public void run(DatabaseEndpoint endpoint, Consumer<StagingTableCommandMapper> action) {
        requireSpVendor(endpoint);
        String key = poolKey(endpoint);
        SqlSessionFactory factory = factories.computeIfAbsent(key, ignored -> createFactory(endpoint, key));
        try (SqlSession session = factory.openSession(true)) {
            action.accept(session.getMapper(StagingTableCommandMapper.class));
        }
    }

    public void dropTable(DatabaseEndpoint endpoint, String schemaName, String tableName) {
        Map<String, Object> params = new HashMap<>();
        params.put("op", "D");
        params.put("schemaName", resolveSchema(endpoint, schemaName));
        params.put("tableName", tableName);
        params.put("columnsDdl", null);
        run(endpoint, mapper -> mapper.executeStgTbl(params));
    }

    public void createTable(
            DatabaseEndpoint endpoint,
            String schemaName,
            String tableName,
            String columnsDdl
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("op", "C");
        params.put("schemaName", resolveSchema(endpoint, schemaName));
        params.put("tableName", tableName);
        params.put("columnsDdl", columnsDdl);
        run(endpoint, mapper -> mapper.executeStgTbl(params));
    }

    public DedupCounts deduplicate(
            DatabaseEndpoint endpoint,
            String schemaName,
            String rawTableName,
            String finalTableName
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("schemaName", resolveSchema(endpoint, schemaName));
        params.put("rawTableName", rawTableName);
        params.put("finalTableName", finalTableName);
        params.put("rawCount", 0L);
        params.put("finalCount", 0L);
        run(endpoint, mapper -> mapper.executeStgDedup(params));
        long rawCount = ((Number) params.get("rawCount")).longValue();
        long finalCount = ((Number) params.get("finalCount")).longValue();
        return new DedupCounts(rawCount, finalCount, rawCount - finalCount);
    }

    public long insertRows(
            DatabaseEndpoint endpoint,
            String schemaName,
            String tableName,
            List<String> columns,
            List<List<Object>> rows
    ) {
        return applyRows(endpoint, "C", schemaName, tableName, columns, rows);
    }

    public long updateRows(
            DatabaseEndpoint endpoint,
            String schemaName,
            String tableName,
            List<String> columns,
            List<List<Object>> rows
    ) {
        return applyRows(endpoint, "U", schemaName, tableName, columns, rows);
    }

    public long deleteRows(
            DatabaseEndpoint endpoint,
            String schemaName,
            String tableName,
            String keyColumn,
            List<Object> keys
    ) {
        List<List<Object>> rows = keys.stream().map(List::<Object>of).toList();
        return applyRows(endpoint, "D", schemaName, tableName, List.of(keyColumn), rows);
    }

    private long applyRows(
            DatabaseEndpoint endpoint,
            String op,
            String schemaName,
            String tableName,
            List<String> columns,
            List<List<Object>> rows
    ) {
        if (rows.isEmpty()) {
            return 0;
        }
        Map<String, Object> payloadBody = new LinkedHashMap<>();
        payloadBody.put("columns", columns);
        payloadBody.put("rows", rows);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(payloadBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("스테이징 행 payload JSON 직렬화에 실패했습니다.", exception);
        }
        Map<String, Object> params = new HashMap<>();
        params.put("op", op);
        params.put("schemaName", resolveSchema(endpoint, schemaName));
        params.put("tableName", tableName);
        params.put("payload", payload);
        run(endpoint, mapper -> mapper.executeStgRows(params));
        return rows.size();
    }

    @PreDestroy
    public void shutdown() {
        factories.clear();
        dataSources.values().forEach(HikariDataSource::close);
        dataSources.clear();
    }

    private SqlSessionFactory createFactory(DatabaseEndpoint endpoint, String key) {
        HikariDataSource dataSource = dataSources.computeIfAbsent(key, ignored -> createDataSource(endpoint));
        Environment environment = new Environment(
                "staging-" + key,
                new JdbcTransactionFactory(),
                dataSource
        );
        Configuration configuration = new Configuration(environment);
        try (InputStream mapperXml = StagingMybatisGateway.class.getResourceAsStream(
                "/mybatis/mapper/staging/StagingTableCommandMapper.xml"
        )) {
            if (mapperXml == null) {
                throw new IllegalStateException("스테이징 MyBatis 매퍼 XML을 찾을 수 없습니다.");
            }
            XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(
                    mapperXml,
                    configuration,
                    "StagingTableCommandMapper.xml",
                    configuration.getSqlFragments()
            );
            xmlMapperBuilder.parse();
        } catch (Exception exception) {
            throw new IllegalStateException("스테이징 MyBatis 매퍼 로드에 실패했습니다.", exception);
        }
        configuration.addMapper(StagingTableCommandMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private HikariDataSource createDataSource(DatabaseEndpoint endpoint) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JdbcUrlFactory.jdbcUrl(endpoint));
        config.setDriverClassName(driverClassName(endpoint.vendor()));
        config.setUsername(endpoint.username());
        config.setPassword(endpoint.password());
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        config.setPoolName("cdw-stg-mybatis-" + endpoint.vendor().name().toLowerCase());
        config.setConnectionTimeout(10_000L);
        return new HikariDataSource(config);
    }

    private static String driverClassName(DatabaseVendor vendor) {
        return switch (vendor) {
            case POSTGRESQL -> "org.postgresql.Driver";
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case MARIADB -> "org.mariadb.jdbc.Driver";
            case ORACLE -> "oracle.jdbc.OracleDriver";
            case CLICKHOUSE -> throw new IllegalArgumentException(
                    "ClickHouse는 sp_cdw_stg_* 대상이 아닙니다."
            );
        };
    }

    private String resolveSchema(DatabaseEndpoint endpoint, String schemaName) {
        if (schemaName != null && !schemaName.isBlank()) {
            return schemaName.trim();
        }
        if (endpoint.schemaName() != null && !endpoint.schemaName().isBlank()) {
            return endpoint.schemaName().trim();
        }
        if (endpoint.vendor() == DatabaseVendor.MYSQL || endpoint.vendor() == DatabaseVendor.MARIADB) {
            return endpoint.database();
        }
        if (endpoint.vendor() == DatabaseVendor.ORACLE) {
            return endpoint.username();
        }
        return "public";
    }

    private String poolKey(DatabaseEndpoint endpoint) {
        return endpoint.vendor() + "|" + endpoint.host() + "|" + endpoint.port() + "|" + endpoint.database() + "|"
                + endpoint.username() + "|pwd=" + Integer.toHexString(Objects.hashCode(endpoint.password()));
    }

    public record DedupCounts(long rawCount, long finalCount, long duplicateCount) {
    }
}
