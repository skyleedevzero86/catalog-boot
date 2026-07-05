package com.sleekydz86.catalog.adapter.outbound.connection;

import com.sleekydz86.catalog.domain.connection.model.ConnectionHealthStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionTestPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@ConditionalOnProperty(
        prefix = "cdw.catalog.connection",
        name = "jdbc-health-check-enabled",
        havingValue = "false"
)
public class StubConnectionTestAdapter implements ConnectionTestPort {

    @Override
    public ConnectionHealthStatus test(ConnectionProfile profile, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return ConnectionHealthStatus.UNHEALTHY;
        }
        return ConnectionHealthStatus.HEALTHY;
    }
}
