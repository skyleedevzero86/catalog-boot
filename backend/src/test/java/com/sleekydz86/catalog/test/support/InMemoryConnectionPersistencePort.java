package com.sleekydz86.catalog.test.support;


import com.sleekydz86.catalog.domain.connection.model.ConnectionHealthStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionLifecycleStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import com.sleekydz86.catalog.domain.connection.port.ConnectionPersistencePort;
import com.sleekydz86.catalog.domain.connection.port.ConnectionTestPort;
import com.sleekydz86.catalog.domain.connection.port.SecretCipherPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryConnectionPersistencePort implements ConnectionPersistencePort {

    private final ConcurrentHashMap<String, ConnectionProfile> store = new ConcurrentHashMap<>();

    @Override
    public ConnectionProfile save(ConnectionProfile profile) {
        ConnectionProfile toSave = profile;
        if (profile.id() == null || profile.id().isBlank()) {
            toSave = new ConnectionProfile(
                    H2EtlIdGenerator.nextId("lnkg"),
                    profile.name(),
                    profile.vendor(),
                    profile.host(),
                    profile.port(),
                    profile.databaseName(),
                    profile.schemaName(),
                    profile.description(),
                    profile.username(),
                    profile.encryptedPassword(),
                    profile.enabled(),
                    profile.lifecycleStatus(),
                    profile.healthStatus(),
                    profile.lastTestAt(),
                    profile.creatorId(),
                    profile.modifierId()
            );
        }
        store.put(toSave.id(), toSave);
        return toSave;
    }

    @Override
    public Optional<ConnectionProfile> findById(String connectionId) {
        return Optional.ofNullable(store.get(connectionId));
    }

    @Override
    public List<ConnectionProfile> findAllActive() {
        return store.values().stream()
                .filter(profile -> profile.lifecycleStatus() != ConnectionLifecycleStatus.DELETED)
                .sorted((a, b) -> b.id().compareTo(a.id()))
                .toList();
    }

    @Override
    public boolean existsByNameExcludingId(String name, String excludeId) {
        return store.values().stream()
                .filter(profile -> profile.lifecycleStatus() != ConnectionLifecycleStatus.DELETED)
                .filter(profile -> excludeId == null || !profile.id().equals(excludeId))
                .anyMatch(profile -> profile.name().equalsIgnoreCase(name));
    }

    public static class PlainSecretCipherPort implements SecretCipherPort {
        @Override
        public String encrypt(String rawSecret) {
            return "enc:" + rawSecret;
        }

        @Override
        public String decrypt(String encryptedSecret) {
            return encryptedSecret.startsWith("enc:") ? encryptedSecret.substring(4) : encryptedSecret;
        }
    }

    public static class HealthyConnectionTestPort implements ConnectionTestPort {
        @Override
        public ConnectionHealthStatus test(ConnectionProfile profile, String rawPassword) {
            return ConnectionHealthStatus.HEALTHY;
        }
    }

    public static ConnectionProfile sampleProfile(String id, String name) {
        return new ConnectionProfile(
                id,
                name,
                DatabaseVendor.POSTGRESQL,
                "localhost",
                5432,
                "cdw",
                "cdw",
                null,
                "postgres",
                "enc:postgres",
                true,
                ConnectionLifecycleStatus.ACTIVE,
                ConnectionHealthStatus.HEALTHY,
                Instant.now(),
                "tester",
                "tester"
        );
    }
}
