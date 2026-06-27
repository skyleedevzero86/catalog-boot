package com.sleekydz86.catalog.domain.connection.service;


import com.sleekydz86.catalog.domain.connection.model.*;
import com.sleekydz86.catalog.domain.connection.port.ConnectionPersistencePort;
import com.sleekydz86.catalog.domain.connection.port.ConnectionTestPort;
import com.sleekydz86.catalog.domain.connection.port.SecretCipherPort;
import com.sleekydz86.catalog.global.exception.ResourceConflictException;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;

import java.time.Instant;

public class ConnectionCommandService {

    private final ConnectionPersistencePort persistencePort;
    private final SecretCipherPort secretCipherPort;
    private final ConnectionTestPort connectionTestPort;

    public ConnectionCommandService(
            ConnectionPersistencePort persistencePort,
            SecretCipherPort secretCipherPort,
            ConnectionTestPort connectionTestPort
    ) {
        this.persistencePort = persistencePort;
        this.secretCipherPort = secretCipherPort;
        this.connectionTestPort = connectionTestPort;
    }

    public ConnectionProfile handle(CreateConnectionCommand command) {
        assertUniqueName(command.name(), null);
        String encrypted = secretCipherPort.encrypt(requirePassword(command.rawPassword()));
        ConnectionProfile created = ConnectionProfile.createNew(
                null,
                command.name().trim(),
                command.vendor(),
                command.host().trim(),
                command.port(),
                command.databaseName().trim(),
                trimToNull(command.schemaName()),
                trimToNull(command.description()),
                command.username().trim(),
                encrypted,
                command.enabled() == null || command.enabled(),
                command.actorId()
        );
        return saveWithTest(created, command.rawPassword());
    }

    public ConnectionProfile handle(UpdateConnectionCommand command) {
        ConnectionProfile profile = requireProfile(command.connectionId());
        if (profile.lifecycleStatus() == com.sleekydz86.catalog.domain.connection.model.ConnectionLifecycleStatus.DELETED) {
            throw new IllegalStateException("삭제된 연결은 수정할 수 없습니다.");
        }
        assertUniqueName(
                command.name() == null || command.name().isBlank() ? profile.name() : command.name().trim(),
                profile.id()
        );

        String encrypted = command.rawPassword() == null || command.rawPassword().isBlank()
                ? null
                : secretCipherPort.encrypt(command.rawPassword());

        ConnectionProfile updated = profile.withUpdate(
                command.name() == null ? profile.name() : command.name().trim(),
                command.host() == null ? profile.host() : command.host().trim(),
                command.port() == null ? profile.port() : command.port(),
                command.databaseName() == null ? profile.databaseName() : command.databaseName().trim(),
                command.schemaName() == null ? profile.schemaName() : trimToNull(command.schemaName()),
                command.description() == null ? profile.description() : trimToNull(command.description()),
                command.enabled() == null ? profile.enabled() : command.enabled(),
                command.username() == null ? profile.username() : command.username().trim(),
                encrypted,
                command.actorId()
        );
        String passwordForTest = command.rawPassword() == null || command.rawPassword().isBlank()
                ? secretCipherPort.decrypt(profile.encryptedPassword())
                : command.rawPassword();
        return saveWithTest(updated, passwordForTest);
    }

    public ConnectionProfile handle(DeleteConnectionCommand command) {
        ConnectionProfile profile = requireProfile(command.connectionId());
        if (profile.lifecycleStatus() == com.sleekydz86.catalog.domain.connection.model.ConnectionLifecycleStatus.DELETED) {
            throw new IllegalStateException("이미 삭제된 연결입니다.");
        }
        ConnectionProfile deleted = profile.markDeleted(command.actorId());
        return persistencePort.save(deleted);
    }

    private ConnectionProfile saveWithTest(ConnectionProfile profile, String rawPassword) {
        ConnectionHealthStatus health = connectionTestPort.test(profile, rawPassword);
        ConnectionProfile tested = profile.withTestResult(health, Instant.now());
        return persistencePort.save(tested);
    }

    private ConnectionProfile requireProfile(String connectionId) {
        return persistencePort.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("연결을 찾을 수 없습니다: " + connectionId));
    }

    private void assertUniqueName(String name, String excludeId) {
        if (persistencePort.existsByNameExcludingId(name.trim(), excludeId)) {
            throw new ResourceConflictException("이미 사용 중인 연결 이름입니다: " + name.trim());
        }
    }

    private String requirePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
        return password;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
