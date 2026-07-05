package com.sleekydz86.catalog.global.application;


import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.model.UpdateConnectionCommand;
import com.sleekydz86.catalog.domain.connection.model.CreateConnectionCommand;
import com.sleekydz86.catalog.domain.connection.model.DeleteConnectionCommand;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionPersistencePort;
import com.sleekydz86.catalog.domain.connection.service.ConnectionCommandService;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConnectionApplicationService {

    private final ConnectionCommandService connectionCommandService;
    private final ConnectionPersistencePort connectionPersistencePort;

    public ConnectionApplicationService(
            ConnectionCommandService connectionCommandService,
            ConnectionPersistencePort connectionPersistencePort
    ) {
        this.connectionCommandService = connectionCommandService;
        this.connectionPersistencePort = connectionPersistencePort;
    }

    @Transactional
    public ConnectionProfile create(CreateConnectionCommand command) {
        return connectionCommandService.handle(command);
    }

    @Transactional
    public ConnectionProfile update(UpdateConnectionCommand command) {
        return connectionCommandService.handle(command);
    }

    @Transactional
    public ConnectionProfile delete(DeleteConnectionCommand command) {
        return connectionCommandService.handle(command);
    }

    public List<ConnectionProfile> list() {
        return connectionPersistencePort.findAllActive();
    }

    public ConnectionProfile get(String connectionId) {
        return connectionPersistencePort.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("연결을 찾을 수 없습니다: " + connectionId));
    }
}
