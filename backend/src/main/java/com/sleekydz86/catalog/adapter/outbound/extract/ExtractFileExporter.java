package com.sleekydz86.catalog.adapter.outbound.extract;

import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcConnectionProvider;
import com.sleekydz86.catalog.adapter.outbound.jdbc.JdbcSqlDialect;
import com.sleekydz86.catalog.domain.extract.model.ExtractCodeMappingSpec;
import com.sleekydz86.catalog.domain.extract.model.ExtractColumnSpec;
import com.sleekydz86.catalog.domain.extract.model.ExtractDatasetManifest;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractExportPort;
import com.sleekydz86.catalog.global.config.ExtractModuleProperties;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.LocalOutputFile;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ExtractFileExporter implements ExtractExportPort {

    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final JdbcConnectionProvider jdbcConnectionProvider;
    private final ExtractModuleProperties extractModuleProperties;

    public ExtractFileExporter(
            JdbcConnectionProvider jdbcConnectionProvider,
            ExtractModuleProperties extractModuleProperties
    ) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
        this.extractModuleProperties = extractModuleProperties;
    }

    @Override
    public List<String> export(ExtractDatasetManifest manifest, ExportRequest request) {
        String format = normalizeFormat(request.outputFormat());
        DatabaseEndpoint staging = request.stagingEndpoint();
        ExportProjection projection = buildProjection(manifest, request.selectedColumnKeys());
        Path outputDir = resolveOutputDir(manifest.datasetId(), request.outputPath());
        try {
            Files.createDirectories(outputDir);
        } catch (IOException exception) {
            throw new IllegalStateException("출력 디렉터리를 만들 수 없습니다: " + outputDir, exception);
        }

        return jdbcConnectionProvider.executeWithRetry(staging, connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(projection.sql())) {
                if ("parquet".equals(format)) {
                    return writeParquetParts(outputDir, manifest.datasetId(), projection.headers(), resultSet, request);
                }
                return writeCsvParts(outputDir, manifest.datasetId(), projection.headers(), resultSet, request);
            } catch (IOException | SQLException exception) {
                throw new IllegalStateException(format.toUpperCase(Locale.ROOT) + " 추출에 실패했습니다.", exception);
            }
        });
    }

    private record ExportProjection(List<String> headers, String sql) {
    }

    private ExportProjection buildProjection(ExtractDatasetManifest manifest, List<String> selectedKeys) {
        Map<String, ExtractColumnSpec> byKey = new LinkedHashMap<>();
        for (ExtractColumnSpec column : manifest.columns()) {
            byKey.put(column.key(), column);
        }
        List<String> keys = selectedKeys == null || selectedKeys.isEmpty()
                ? manifest.columns().stream().map(ExtractColumnSpec::key).toList()
                : selectedKeys;

        List<String> headers = new ArrayList<>();
        List<String> expressions = new ArrayList<>();
        for (String key : keys) {
            ExtractColumnSpec column = byKey.get(key);
            if (column == null) {
                continue;
            }
            String physical = physicalForKey(manifest, key);
            headers.add(label(column));
            expressions.add("s." + JdbcSqlDialect.quoteIdentifier(manifest.stagingVendor(), physical));
            ExtractCodeMappingSpec mapping = mappingForKey(manifest, key);
            if (mapping != null) {
                int mappingIndex = indexOfMapping(manifest, mapping);
                headers.add(mapping.codeNameLabel() == null ? mapping.codeNameColumnKey() : mapping.codeNameLabel());
                expressions.add("m" + mappingIndex + "."
                        + JdbcSqlDialect.quoteIdentifier(manifest.stagingVendor(), "code_name"));
            }
        }

        String stagingQualified = JdbcSqlDialect.qualifiedName(
                manifest.stagingVendor(), manifest.stagingSchema(), manifest.stagingTableName()
        );
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(String.join(", ", expressions))
                .append(" FROM ").append(stagingQualified).append(" s");
        for (int i = 0; i < manifest.codeMappings().size(); i++) {
            ExtractCodeMappingSpec mapping = manifest.codeMappings().get(i);
            String mappingQualified = JdbcSqlDialect.qualifiedName(
                    manifest.stagingVendor(), manifest.stagingSchema(), manifest.mappingTableNames().get(i)
            );
            String physical = physicalForKey(manifest, mapping.sourceColumnKey());
            sql.append(" LEFT JOIN ").append(mappingQualified).append(" m").append(i)
                    .append(" ON m").append(i).append(".")
                    .append(JdbcSqlDialect.quoteIdentifier(manifest.stagingVendor(), "code"))
                    .append(" = s.").append(JdbcSqlDialect.quoteIdentifier(manifest.stagingVendor(), physical));
        }
        sql.append(" ORDER BY s.").append(JdbcSqlDialect.quoteIdentifier(manifest.stagingVendor(), "__row_no"));
        return new ExportProjection(headers, sql.toString());
    }

    private int indexOfMapping(ExtractDatasetManifest manifest, ExtractCodeMappingSpec mapping) {
        for (int i = 0; i < manifest.codeMappings().size(); i++) {
            if (manifest.codeMappings().get(i).sourceColumnKey().equals(mapping.sourceColumnKey())) {
                return i;
            }
        }
        return 0;
    }

    private ExtractCodeMappingSpec mappingForKey(ExtractDatasetManifest manifest, String key) {
        for (ExtractCodeMappingSpec mapping : manifest.codeMappings()) {
            if (mapping.sourceColumnKey().equals(key)) {
                return mapping;
            }
        }
        return null;
    }

    private String physicalForKey(ExtractDatasetManifest manifest, String key) {
        for (int i = 0; i < manifest.columns().size(); i++) {
            if (manifest.columns().get(i).key().equals(key)) {
                return manifest.physicalColumnNames().get(i);
            }
        }
        throw new IllegalArgumentException("컬럼을 찾을 수 없습니다: " + key);
    }

    private String label(ExtractColumnSpec column) {
        return column.label() == null || column.label().isBlank() ? column.key() : column.label();
    }

    private List<String> writeCsvParts(
            Path outputDir,
            String datasetId,
            List<String> headers,
            ResultSet resultSet,
            ExportRequest request
    ) throws IOException, SQLException {
        List<String> paths = new ArrayList<>();
        int part = 1;
        int rowsInPart = 0;
        BufferedWriter writer = null;
        Path tempFile = null;
        try {
            while (resultSet.next()) {
                if (writer == null || (!request.singleFile() && rowsInPart >= request.maxRowsPerFile())) {
                    if (writer != null) {
                        writer.close();
                        paths.add(finalizePart(outputDir, datasetId, part++, tempFile, "csv").toString());
                    }
                    tempFile = outputDir.resolve(datasetId + ".tmp-" + part + ".csv");
                    writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8);
                    writer.write(new String(UTF8_BOM, StandardCharsets.UTF_8));
                    if (request.includeHeader()) {
                        writer.write(headers.stream().map(this::escapeCsv).collect(Collectors.joining(",")));
                        writer.newLine();
                    }
                    rowsInPart = 0;
                }
                List<String> values = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    Object value = resultSet.getObject(i + 1);
                    values.add(escapeCsv(value == null ? "" : String.valueOf(value)));
                }
                writer.write(String.join(",", values));
                writer.newLine();
                rowsInPart++;
            }
            if (writer != null) {
                writer.close();
                paths.add(finalizePart(outputDir, datasetId, part, tempFile, "csv").toString());
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return paths;
    }

    private List<String> writeParquetParts(
            Path outputDir,
            String datasetId,
            List<String> headers,
            ResultSet resultSet,
            ExportRequest request
    ) throws IOException, SQLException {
        List<String> paths = new ArrayList<>();
        int part = 1;
        int rowsInPart = 0;
        ParquetWriter<GenericRecord> writer = null;
        Path tempFile = null;
        Schema schema = parquetSchema(headers);
        try {
            while (resultSet.next()) {
                if (writer == null || (!request.singleFile() && rowsInPart >= request.maxRowsPerFile())) {
                    if (writer != null) {
                        writer.close();
                        paths.add(finalizePart(outputDir, datasetId, part++, tempFile, "parquet").toString());
                    }
                    tempFile = outputDir.resolve(datasetId + ".tmp-" + part + ".parquet");
                    writer = AvroParquetWriter.<GenericRecord>builder(new LocalOutputFile(tempFile))
                            .withSchema(schema)
                            .withCompressionCodec(CompressionCodecName.SNAPPY)
                            .build();
                    rowsInPart = 0;
                }
                GenericRecord record = new GenericData.Record(schema);
                for (int i = 0; i < headers.size(); i++) {
                    Object value = resultSet.getObject(i + 1);
                    record.put(parquetFieldName(i), value == null ? null : String.valueOf(value));
                }
                writer.write(record);
                rowsInPart++;
            }
            if (writer != null) {
                writer.close();
                paths.add(finalizePart(outputDir, datasetId, part, tempFile, "parquet").toString());
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return paths;
    }

    private Schema parquetSchema(List<String> headers) {
        SchemaBuilder.FieldAssembler<Schema> fields = SchemaBuilder.record("extract_row").fields();
        for (int i = 0; i < headers.size(); i++) {
            fields = fields.name(parquetFieldName(i))
                    .type().nullable().stringType()
                    .noDefault();
        }
        return fields.endRecord();
    }

    private String parquetFieldName(int index) {
        return "col_" + index;
    }

    private Path finalizePart(Path outputDir, String datasetId, int part, Path tempFile, String extension)
            throws IOException {
        Path finalFile = outputDir.resolve(datasetId + "_" + String.format("%03d", part) + "." + extension);
        Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
        return finalFile;
    }

    private Path resolveOutputDir(String datasetId, String outputPath) {
        if (outputPath != null && !outputPath.isBlank()) {
            return Path.of(outputPath);
        }
        return Path.of(extractModuleProperties.exportRoot(), datasetId);
    }

    private String normalizeFormat(String outputFormat) {
        if (outputFormat == null || outputFormat.isBlank()) {
            return "csv";
        }
        String normalized = outputFormat.trim().toLowerCase(Locale.ROOT);
        if (!"csv".equals(normalized) && !"parquet".equals(normalized)) {
            throw new IllegalArgumentException("지원하지 않는 출력 형식입니다: " + outputFormat);
        }
        return normalized;
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
