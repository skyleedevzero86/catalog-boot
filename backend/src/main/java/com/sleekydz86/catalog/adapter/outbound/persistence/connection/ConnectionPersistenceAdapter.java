package com.sleekydz86.catalog.adapter.outbound.persistence.connection;

import com.sleekydz86.catalog.domain.connection.model.ConnectionLifecycleStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionPersistencePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class ConnectionPersistenceAdapter implements ConnectionPersistencePort {

    private final ConnectionCommandMapper commandMapper;

    public ConnectionPersistenceAdapter(ConnectionCommandMapper commandMapper) {
        this.commandMapper = commandMapper;
    }

    @Override
    @Transactional
    public ConnectionProfile save(ConnectionProfile profile) {
        String op = resolveOp(profile);
        var params = ConnectionPersistenceMapper.toProcedureParams(profile, op);
        params.put("op", op);
        commandMapper.executeLnkgProfile(params);
        String id = profile.id() != null && !profile.id().isBlank()
                ? profile.id()
                : (String) params.get("lnkgId");
        return ConnectionPersistenceMapper.toDomain(commandMapper.selectById(id));
    }

    @Override
    public Optional<ConnectionProfile> findById(String connectionId) {
        return Optional.ofNullable(ConnectionPersistenceMapper.toDomain(commandMapper.selectById(connectionId)));
    }

    @Override
    public List<ConnectionProfile> findAllActive() {
        return commandMapper.selectActiveList().stream()
                .map(ConnectionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByNameExcludingId(String name, String excludeId) {
        return commandMapper.existsByName(name, excludeId);
    }

    private String resolveOp(ConnectionProfile profile) {
        if (profile.lifecycleStatus() == ConnectionLifecycleStatus.DELETED) {
            return "D";
        }
        if (profile.id() == null || profile.id().isBlank()) {
            return "C";
        }
        return "U";
    }
}
