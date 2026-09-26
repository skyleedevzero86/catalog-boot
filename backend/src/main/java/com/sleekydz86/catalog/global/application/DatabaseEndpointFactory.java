package com.sleekydz86.catalog.global.application;

import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionPersistencePort;
import com.sleekydz86.catalog.domain.connection.port.out.SecretCipherPort;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class DatabaseEndpointFactory {

    private final ConnectionPersistencePort connectionPersistencePort;
    private final SecretCipherPort secretCipherPort;

    public DatabaseEndpointFactory(
            ConnectionPersistencePort connectionPersistencePort,
            SecretCipherPort secretCipherPort
    ) {
        this.connectionPersistencePort = connectionPersistencePort;
        this.secretCipherPort = secretCipherPort;
    }

    public DatabaseEndpoint requireEndpoint(String connectionId) {
        ConnectionProfile profile = connectionPersistencePort.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("연결을 찾을 수 없습니다: " + connectionId));
        return toEndpoint(profile);
    }

    public DatabaseEndpoint toEndpoint(ConnectionProfile profile) {
        return new DatabaseEndpoint(
                profile.vendor(),
                profile.host(),
                profile.port(),
                profile.databaseName(),
                profile.schemaName(),
                profile.username(),
                secretCipherPort.decrypt(profile.encryptedPassword())
        );
    }
}
