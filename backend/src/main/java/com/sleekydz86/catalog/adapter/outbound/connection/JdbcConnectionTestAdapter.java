package com.sleekydz86.catalog.adapter.outbound.connection;

import com.sleekydz86.catalog.domain.connection.model.ConnectionHealthStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.port.ConnectionTestPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.sql.Connection;
import java.sql.DriverManager;

@Component
@Primary
@ConditionalOnProperty(
        prefix = "com.sleekydz86.catalog.connection",
        name = "jdbc-health-check-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class JdbcConnectionTestAdapter implements ConnectionTestPort {

    private final JdbcConnectionProvider jdbcConnectionProvider;

    public JdbcConnectionTestAdapter(JdbcConnectionProvider jdbcConnectionProvider) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
    }

    @Override
    public ConnectionHealthStatus test(ConnectionProfile profile, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
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
            return Boolean.TRUE.equals(valid) ? ConnectionHealthStatus.HEALTHY : ConnectionHealthStatus.UNHEALTHY;
        } catch (RuntimeException exception) {
            try (Connection connection = DriverManager.getConnection(
                    JdbcUrlFactory.jdbcUrl(endpoint),
                    endpoint.username(),
                    endpoint.password()
            )) {
                return connection.isValid(3) ? ConnectionHealthStatus.HEALTHY : ConnectionHealthStatus.UNHEALTHY;
            } catch (Exception fallbackException) {
                return ConnectionHealthStatus.UNHEALTHY;
            }
        }
    }
}
