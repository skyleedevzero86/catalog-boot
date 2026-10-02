package com.sleekydz86.catalog.adapter.outbound.connection;

import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcConnectionProvider;
import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcUrlFactory;
import com.sleekydz86.catalog.domain.connection.model.ConnectionHealthStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionTestPort;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
@Primary
@ConditionalOnProperty(
        prefix = "com.sleekydz86.catalog.connection",
        name = "jdbc-health-check-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class JdbcConnectionTestAdapter implements ConnectionTestPort {

    private static final Logger log = LoggerFactory.getLogger(JdbcConnectionTestAdapter.class);

    private final JdbcConnectionProvider jdbcConnectionProvider;

    public JdbcConnectionTestAdapter(JdbcConnectionProvider jdbcConnectionProvider) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
    }

    @Override
    public ConnectionHealthStatus test(ConnectionProfile profile, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            log.warn("연결 헬스체크 실패: 비밀번호 없음 connectionId={} vendor={}",
                    profile.id(), profile.vendor());
            return ConnectionHealthStatus.UNHEALTHY;
        }
        DatabaseEndpoint endpoint = new DatabaseEndpoint(
                profile.vendor(),
                profile.host(),
                profile.port(),
                profile.databaseName(),
                profile.schemaName(),
                profile.username(),
                rawPassword
        );
        try {
            Boolean valid = jdbcConnectionProvider.executeWithRetry(
                    endpoint,
                    connection -> connection.isValid(3)
            );
            ConnectionHealthStatus status = Boolean.TRUE.equals(valid)
                    ? ConnectionHealthStatus.HEALTHY
                    : ConnectionHealthStatus.UNHEALTHY;
            if (status == ConnectionHealthStatus.UNHEALTHY) {
                log.warn("연결 헬스체크 실패(isValid=false) connectionId={} vendor={} host={}:{}",
                        profile.id(), profile.vendor(), profile.host(), profile.port());
            }
            return status;
        } catch (RuntimeException primaryException) {
            log.warn(
                    "연결 헬스체크 1차 실패, fallback 시도 connectionId={} vendor={} host={}:{} cause={}",
                    profile.id(),
                    profile.vendor(),
                    profile.host(),
                    profile.port(),
                    primaryException.getMessage()
            );
            try (Connection connection = DriverManager.getConnection(
                    JdbcUrlFactory.jdbcUrl(endpoint),
                    endpoint.username(),
                    endpoint.password()
            )) {
                boolean valid = connection.isValid(3);
                if (!valid) {
                    log.warn("연결 헬스체크 fallback isValid=false connectionId={}", profile.id());
                }
                return valid ? ConnectionHealthStatus.HEALTHY : ConnectionHealthStatus.UNHEALTHY;
            } catch (SQLException fallbackException) {
                log.error(
                        "연결 헬스체크 fallback 실패 connectionId={} vendor={} host={}:{}",
                        profile.id(),
                        profile.vendor(),
                        profile.host(),
                        profile.port(),
                        fallbackException
                );
                return ConnectionHealthStatus.UNHEALTHY;
            }
        }
    }
}
