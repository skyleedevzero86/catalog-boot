package com.sleekydz86.catalog.domain.connection.port.out;


import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import java.util.List;
import java.util.Optional;

public interface ConnectionPersistencePort {

    ConnectionProfile save(ConnectionProfile profile);

    Optional<ConnectionProfile> findById(String connectionId);

    List<ConnectionProfile> findAllActive();

    boolean existsByNameExcludingId(String name, String excludeId);
}
