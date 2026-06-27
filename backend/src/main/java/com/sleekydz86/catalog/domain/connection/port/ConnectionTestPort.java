package com.sleekydz86.catalog.domain.connection.port;

import com.sleekydz86.catalog.domain.connection.model.ConnectionHealthStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;

public interface ConnectionTestPort {

    ConnectionHealthStatus test(ConnectionProfile profile, String rawPassword);
}
