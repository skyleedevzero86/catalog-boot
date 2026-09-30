package com.sleekydz86.catalog.adapter.outbound.connection;

import com.sleekydz86.catalog.domain.connection.port.out.ConnectionEndpointPort;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.global.application.DatabaseEndpointFactory;
import org.springframework.stereotype.Component;

@Component
public class ConnectionEndpointAdapter implements ConnectionEndpointPort {

    private final DatabaseEndpointFactory databaseEndpointFactory;

    public ConnectionEndpointAdapter(DatabaseEndpointFactory databaseEndpointFactory) {
        this.databaseEndpointFactory = databaseEndpointFactory;
    }

    @Override
    public DatabaseEndpoint requireEndpoint(String connectionId) {
        return databaseEndpointFactory.requireEndpoint(connectionId);
    }
}
