package com.sleekydz86.catalog.domain.connection.port.out;

import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;

public interface ConnectionEndpointPort {

    DatabaseEndpoint requireEndpoint(String connectionId);
}
