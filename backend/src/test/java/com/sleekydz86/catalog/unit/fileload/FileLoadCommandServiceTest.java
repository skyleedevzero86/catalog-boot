package com.sleekydz86.catalog.unit.fileload;

import com.sleekydz86.catalog.domain.connection.model.ConnectionHealthStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.port.out.ConnectionTestPort;
import com.sleekydz86.catalog.domain.fileload.model.ConnectionProbeResult;
import com.sleekydz86.catalog.domain.fileload.model.FileColumnDef;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetFormat;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetTemplate;
import com.sleekydz86.catalog.domain.fileload.port.out.FileLoadTargetPort;
import com.sleekydz86.catalog.domain.fileload.port.out.SpreadsheetDocumentPort;
import com.sleekydz86.catalog.domain.fileload.service.FileLoadCommandService;
import com.sleekydz86.catalog.domain.migration.model.ColumnSchema;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.migration.model.SourceTableDescriptor;
import com.sleekydz86.catalog.domain.migration.model.TableSchema;
import com.sleekydz86.catalog.domain.migration.port.out.SourceMetadataPort;
import com.sleekydz86.catalog.global.exception.BusinessException;
import com.sleekydz86.catalog.global.exception.ErrorCode;
import com.sleekydz86.catalog.test.support.InMemoryConnectionPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("파일 적재 명령 서비스 단위 테스트")
class FileLoadCommandServiceTest {

    private InMemoryConnectionPersistencePort connections;
    private MutableConnectionTestPort connectionTest;
    private FakeSourceMetadata metadata;
    private FakeFileLoadTarget target;
    private FakeSpreadsheetDocument spreadsheet;
    private FileLoadCommandService service;

    @BeforeEach
    void setUp() {
        connections = new InMemoryConnectionPersistencePort();
        connectionTest = new MutableConnectionTestPort();
        metadata = new FakeSourceMetadata();
        target = new FakeFileLoadTarget();
        spreadsheet = new FakeSpreadsheetDocument();
        service = new FileLoadCommandService(
                connections,
                connectionTest,
                new InMemoryConnectionPersistencePort.PlainSecretCipherPort(),
                metadata,
                target,
                spreadsheet
        );
        connections.save(InMemoryConnectionPersistencePort.sampleProfile("lnkg-1", "demo"));
        connectionTest.status = ConnectionHealthStatus.HEALTHY;
    }

    @Test
    @DisplayName("접속 성공 시 connected=true")
    void probe_healthyConnection_returnsConnected() {
        ConnectionProbeResult result = service.probe("lnkg-1");

        assertThat(result.connected()).isTrue();
        assertThat(result.message()).contains("성공");
    }

    @Test
    @DisplayName("접속 실패 시 connected=false")
    void probe_unhealthyConnection_returnsNotConnected() {
        connectionTest.status = ConnectionHealthStatus.UNHEALTHY;

        ConnectionProbeResult result = service.probe("lnkg-1");

        assertThat(result.connected()).isFalse();
        assertThat(result.message()).contains("실패");
    }

    @Test
    @DisplayName("데이터 행이 없으면 VALIDATION_FAILED")
    void upload_emptyRows_throwsValidation() {
        metadata.table = new TableSchema(
                "public",
                "demo",
                List.of(new ColumnSchema("id", "INTEGER", 4, 10, 0, true, false, 1))
        );
        spreadsheet.rows = List.of();

        assertThatThrownBy(() -> service.upload(
                "lnkg-1",
                "public",
                "demo",
                SpreadsheetFormat.CSV,
                new ByteArrayInputStream("id\n".getBytes())
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).code())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("파싱된 행을 적재한다")
    void upload_insertsParsedRows() {
        metadata.table = new TableSchema(
                "public",
                "demo",
                List.of(new ColumnSchema("id", "INTEGER", 4, 10, 0, true, false, 1))
        );
        spreadsheet.rows = List.of(List.of("1"), List.of("2"));

        var result = service.upload(
                "lnkg-1",
                "public",
                "demo",
                SpreadsheetFormat.CSV,
                new ByteArrayInputStream(new byte[0])
        );

        assertThat(result.insertedRows()).isEqualTo(2);
        assertThat(target.lastRows).hasSize(2);
    }

    private static final class MutableConnectionTestPort implements ConnectionTestPort {
        private ConnectionHealthStatus status = ConnectionHealthStatus.HEALTHY;

        @Override
        public ConnectionHealthStatus test(ConnectionProfile profile, String rawPassword) {
            return status;
        }
    }

    private static final class FakeSourceMetadata implements SourceMetadataPort {
        private TableSchema table;

        @Override
        public TableSchema readTable(DatabaseEndpoint source, String schemaName, String tableName) {
            return table;
        }

        @Override
        public List<SourceTableDescriptor> listTables(DatabaseEndpoint source, String schemaName) {
            return List.of();
        }
    }

    private static final class FakeFileLoadTarget implements FileLoadTargetPort {
        private List<List<Object>> lastRows = List.of();

        @Override
        public void createTable(
                DatabaseEndpoint target,
                String schemaName,
                String tableName,
                String tableComment,
                List<FileColumnDef> columns
        ) {
        }

        @Override
        public long insertRows(
                DatabaseEndpoint target,
                String schemaName,
                String tableName,
                List<String> columnNames,
                List<List<Object>> rows
        ) {
            lastRows = rows;
            return rows.size();
        }
    }

    private static final class FakeSpreadsheetDocument implements SpreadsheetDocumentPort {
        private List<List<String>> rows = List.of();

        @Override
        public SpreadsheetTemplate buildTemplate(String tableName, List<String> columnNames, SpreadsheetFormat format) {
            return new SpreadsheetTemplate(tableName + ".csv", "text/csv", new byte[0], columnNames);
        }

        @Override
        public List<List<String>> readDataRows(
                InputStream inputStream,
                SpreadsheetFormat format,
                List<String> expectedColumns
        ) {
            return rows;
        }
    }
}
