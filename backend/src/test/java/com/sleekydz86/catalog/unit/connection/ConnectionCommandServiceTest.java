package com.sleekydz86.catalog.unit.connection;

import com.sleekydz86.catalog.domain.connection.model.*;
import com.sleekydz86.catalog.domain.connection.service.ConnectionCommandService;
import com.sleekydz86.catalog.global.exception.ResourceConflictException;
import com.sleekydz86.catalog.test.support.InMemoryConnectionPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("연결 명령 서비스 단위 테스트")
class ConnectionCommandServiceTest {

    private InMemoryConnectionPersistencePort persistencePort;
    private ConnectionCommandService commandService;

    @BeforeEach
    void setUp() {
        persistencePort = new InMemoryConnectionPersistencePort();
        commandService = new ConnectionCommandService(
                persistencePort,
                new InMemoryConnectionPersistencePort.PlainSecretCipherPort(),
                new InMemoryConnectionPersistencePort.HealthyConnectionTestPort()
        );
    }

    @Test
    @DisplayName("연결을 등록하고 헬스 체크 결과를 반영한다")
    void createConnection() {
        var created = commandService.handle(new CreateConnectionCommand(
                "Primary Postgres",
                DatabaseVendor.POSTGRESQL,
                "localhost",
                5432,
                "cdw",
                "cdw",
                "test",
                "postgres",
                "postgres",
                true,
                "admin"
        ));

        assertThat(created.name()).isEqualTo("Primary Postgres");
        assertThat(created.healthStatus()).isEqualTo(ConnectionHealthStatus.HEALTHY);
        assertThat(created.lifecycleStatus()).isEqualTo(ConnectionLifecycleStatus.ACTIVE);
    }

    @Test
    @DisplayName("중복 연결 이름을 거부한다")
    void rejectDuplicateName() {
        commandService.handle(sampleCreate("dup-name"));
        assertThatThrownBy(() -> commandService.handle(sampleCreate("dup-name")))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    @DisplayName("연결 프로필을 삭제 상태로 표시한다")
    void deleteConnection() {
        var created = commandService.handle(sampleCreate("to-delete"));
        var deleted = commandService.handle(new DeleteConnectionCommand(created.id(), "admin"));
        assertThat(deleted.lifecycleStatus()).isEqualTo(ConnectionLifecycleStatus.DELETED);
    }

    private CreateConnectionCommand sampleCreate(String name) {
        return new CreateConnectionCommand(
                name,
                DatabaseVendor.POSTGRESQL,
                "localhost",
                5432,
                "cdw",
                null,
                null,
                "postgres",
                "postgres",
                true,
                "admin"
        );
    }
}
