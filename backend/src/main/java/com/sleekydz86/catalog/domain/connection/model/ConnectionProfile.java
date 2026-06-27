package com.sleekydz86.catalog.domain.connection.model;


import java.time.Instant;

public record ConnectionProfile(
        String id,
        String name,
        DatabaseVendor vendor,
        String host,
        int port,
        String databaseName,
        String schemaName,
        String description,
        String username,
        String encryptedPassword,
        boolean enabled,
        ConnectionLifecycleStatus lifecycleStatus,
        ConnectionHealthStatus healthStatus,
        Instant lastTestAt,
        String creatorId,
        String modifierId
) {

    public static ConnectionProfile createNew(
            String id,
            String name,
            DatabaseVendor vendor,
            String host,
            int port,
            String databaseName,
            String schemaName,
            String description,
            String username,
            String encryptedPassword,
            boolean enabled,
            String userId
    ) {
        ConnectionLifecycleStatus lifecycle = enabled ? ConnectionLifecycleStatus.ACTIVE : ConnectionLifecycleStatus.DISABLED;
        return new ConnectionProfile(
                id,
                name,
                vendor,
                host,
                port,
                databaseName,
                schemaName,
                description,
                username,
                encryptedPassword,
                enabled,
                lifecycle,
                ConnectionHealthStatus.VALIDATING,
                null,
                userId,
                userId
        );
    }

    public ConnectionProfile withUpdate(
            String name,
            String host,
            int port,
            String databaseName,
            String schemaName,
            String description,
            boolean enabled,
            String username,
            String encryptedPassword,
            String userId
    ) {
        ConnectionLifecycleStatus lifecycle = enabled ? ConnectionLifecycleStatus.ACTIVE : ConnectionLifecycleStatus.DISABLED;
        return new ConnectionProfile(
                id,
                name,
                vendor,
                host,
                port,
                databaseName,
                schemaName,
                description,
                username == null ? this.username : username,
                encryptedPassword == null ? this.encryptedPassword : encryptedPassword,
                enabled,
                lifecycle,
                ConnectionHealthStatus.VALIDATING,
                lastTestAt,
                creatorId,
                userId
        );
    }

    public ConnectionProfile withTestResult(ConnectionHealthStatus healthStatus, Instant testedAt) {
        return new ConnectionProfile(
                id, name, vendor, host, port, databaseName, schemaName, description,
                username, encryptedPassword, enabled, lifecycleStatus, healthStatus, testedAt,
                creatorId, modifierId
        );
    }

    public ConnectionProfile markDeleted(String userId) {
        return new ConnectionProfile(
                id, name, vendor, host, port, databaseName, schemaName, description,
                username, encryptedPassword, false, ConnectionLifecycleStatus.DELETED,
                healthStatus, lastTestAt, creatorId, userId
        );
    }
}
