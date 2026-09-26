package com.sleekydz86.catalog.adapter.outbound.jdbc;

import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.global.config.ConnectionModuleProperties;
import com.sleekydz86.catalog.global.config.MigrationJdbcProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
public class JdbcConnectionProvider {

    private final ConnectionModuleProperties connectionProperties;
    private final MigrationJdbcProperties migrationJdbcProperties;
    private final ConcurrentHashMap<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    public JdbcConnectionProvider(
            ConnectionModuleProperties connectionProperties,
            MigrationJdbcProperties migrationJdbcProperties
    ) {
        this.connectionProperties = connectionProperties;
        this.migrationJdbcProperties = migrationJdbcProperties;
    }

    public Connection openConnection(DatabaseEndpoint endpoint) throws SQLException {
        return poolFor(endpoint).getConnection();
    }

    public <T> T executeWithRetry(DatabaseEndpoint endpoint, SqlFunction<T> action) {
        int attempts = migrationJdbcProperties.maxRetries();
        RuntimeException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try (Connection connection = openConnection(endpoint)) {
                return action.apply(connection);
            } catch (RuntimeException exception) {
                last = exception;
                if (attempt >= attempts) {
                    throw exception;
                }
                sleep(migrationJdbcProperties.retryDelayMs());
            } catch (SQLException exception) {
                last = new IllegalStateException("JDBC 연결에 실패했습니다.", exception);
                if (attempt >= attempts) {
                    throw last;
                }
                sleep(migrationJdbcProperties.retryDelayMs());
            }
        }
        throw last == null ? new IllegalStateException("JDBC 실행에 실패했습니다.") : last;
    }

    public void runWithRetry(DatabaseEndpoint endpoint, SqlConsumer action) {
        executeWithRetry(endpoint, connection -> {
            action.accept(connection);
            return null;
        });
    }

    @PreDestroy
    public void shutdown() {
        pools.values().forEach(HikariDataSource::close);
        pools.clear();
    }

    private HikariDataSource poolFor(DatabaseEndpoint endpoint) {
        String key = endpoint.vendor() + "|" + endpoint.host() + "|" + endpoint.port()
                + "|" + endpoint.database() + "|" + endpoint.username();
        return pools.computeIfAbsent(key, ignored -> createPool(endpoint));
    }

    private HikariDataSource createPool(DatabaseEndpoint endpoint) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JdbcUrlFactory.jdbcUrl(endpoint));
        config.setUsername(endpoint.username());
        config.setPassword(endpoint.password());
        config.setMaximumPoolSize(migrationJdbcProperties.poolMaxSize());
        config.setMinimumIdle(1);
        config.setPoolName("cdw-mig-" + endpoint.vendor().name().toLowerCase());
        long connectTimeoutMs = connectionProperties.connectTimeout().toMillis();
        config.setConnectionTimeout(connectTimeoutMs);
        config.setValidationTimeout(Math.min(connectTimeoutMs, 5000L));
        return new HikariDataSource(config);
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("JDBC 재시도가 중단되었습니다.", exception);
        }
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }
}
