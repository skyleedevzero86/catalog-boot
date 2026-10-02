package com.sleekydz86.catalog.adapter.outbound.fileload;

import com.sleekydz86.catalog.domain.fileload.model.ExportedTableData;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetDbExport;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetFormat;
import com.sleekydz86.catalog.domain.fileload.model.SpreadsheetTemplate;
import com.sleekydz86.catalog.domain.fileload.port.out.SpreadsheetDocumentPort;
import com.sleekydz86.catalog.global.exception.BusinessException;
import com.sleekydz86.catalog.global.exception.ErrorCode;
import com.sleekydz86.catalog.global.exception.InfrastructureException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class ApachePoiSpreadsheetAdapter implements SpreadsheetDocumentPort {

    private final DataFormatter dataFormatter = new DataFormatter();

    @Override
    public SpreadsheetTemplate buildTemplate(String tableName, List<String> columnNames, SpreadsheetFormat format) {
        try {
            byte[] content = switch (format) {
                case CSV -> buildCsv(columnNames);
                case XLSX -> buildWorkbook(columnNames, new XSSFWorkbook());
                case XLS -> buildWorkbook(columnNames, new HSSFWorkbook());
            };
            return new SpreadsheetTemplate(
                    tableName + "_template." + format.fileExtension(),
                    format.contentType(),
                    content,
                    columnNames
            );
        } catch (IOException exception) {
            throw InfrastructureException.of(
                    ErrorCode.FILE_LOAD_FAILED,
                    "엑셀/CSV 템플릿 생성에 실패했습니다. format=" + format,
                    exception
            );
        }
    }

    @Override
    public List<List<String>> readDataRows(
            InputStream inputStream,
            SpreadsheetFormat format,
            List<String> expectedColumns
    ) {
        try {
            return switch (format) {
                case CSV -> readCsv(inputStream, expectedColumns);
                case XLSX -> readWorkbook(new XSSFWorkbook(inputStream), expectedColumns);
                case XLS -> readWorkbook(new HSSFWorkbook(inputStream), expectedColumns);
            };
        } catch (IOException exception) {
            throw InfrastructureException.of(
                    ErrorCode.FILE_LOAD_FAILED,
                    "업로드 파일 파싱에 실패했습니다. format=" + format,
                    exception
            );
        }
    }

    private byte[] buildCsv(List<String> columnNames) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
            writer.write(String.join(",", columnNames.stream().map(this::escapeCsv).toList()));
            writer.write("\n");
        }
        return output.toByteArray();
    }

    private byte[] buildWorkbook(List<String> columnNames, Workbook workbook) throws IOException {
        Sheet sheet = workbook.createSheet("data");
        Row header = sheet.createRow(0);
        for (int i = 0; i < columnNames.size(); i++) {
            header.createCell(i).setCellValue(columnNames.get(i));
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        workbook.write(output);
        workbook.close();
        return output.toByteArray();
    }

    private List<List<String>> readCsv(InputStream inputStream, List<String> expectedColumns) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "CSV 헤더가 없습니다.");
            }
            List<String> headers = parseCsvLine(headerLine);
            validateHeaders(headers, expectedColumns);
            List<List<String>> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                rows.add(alignRow(values, expectedColumns.size()));
            }
            return rows;
        }
    }

    private List<List<String>> readWorkbook(Workbook workbook, List<String> expectedColumns) throws IOException {
        try (workbook) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "엑셀 시트가 없습니다.");
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "엑셀 헤더 행이 없습니다.");
            }
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < expectedColumns.size(); i++) {
                headers.add(cellText(headerRow.getCell(i)));
            }
            validateHeaders(headers, expectedColumns);
            List<List<String>> rows = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isEmptyRow(row, expectedColumns.size())) {
                    continue;
                }
                List<String> values = new ArrayList<>(expectedColumns.size());
                for (int c = 0; c < expectedColumns.size(); c++) {
                    values.add(cellText(row.getCell(c)));
                }
                rows.add(values);
            }
            return rows;
        }
    }

    private void validateHeaders(List<String> headers, List<String> expectedColumns) {
        if (headers.size() < expectedColumns.size()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "헤더 컬럼 수가 부족합니다. expected=" + expectedColumns.size() + " actual=" + headers.size()
            );
        }
        for (int i = 0; i < expectedColumns.size(); i++) {
            String expected = expectedColumns.get(i);
            String actual = headers.get(i) == null ? "" : headers.get(i).trim();
            if (!expected.equalsIgnoreCase(actual)) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "헤더가 양식과 일치하지 않습니다. index=" + i
                                + " expected=" + expected + " actual=" + actual
                );
            }
        }
    }

    private List<String> alignRow(List<String> values, int size) {
        List<String> aligned = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            aligned.add(i < values.size() ? values.get(i) : "");
        }
        return aligned;
    }

    private boolean isEmptyRow(Row row, int size) {
        for (int i = 0; i < size; i++) {
            if (!cellText(row.getCell(i)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    @Override
    public SpreadsheetDbExport buildDbExport(
            SpreadsheetFormat format,
            String connectionId,
            String extractedBy,
            Instant extractedAt,
            List<ExportedTableData> tables
    ) {
        if (tables == null || tables.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "추출할 테이블이 없습니다.");
        }
        if (format == SpreadsheetFormat.CSV && tables.size() > 1) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "CSV는 테이블 1개만 지원합니다. 여러 테이블은 xlsx 또는 xls를 선택하세요."
            );
        }
        try {
            byte[] content = switch (format) {
                case CSV -> buildCsvDbExport(connectionId, extractedBy, extractedAt, tables.get(0));
                case XLSX -> buildWorkbookDbExport(new XSSFWorkbook(), connectionId, extractedBy, extractedAt, tables, false);
                case XLS -> buildWorkbookDbExport(new HSSFWorkbook(), connectionId, extractedBy, extractedAt, tables, true);
            };
            long total = tables.stream().mapToLong(ExportedTableData::rowCount).sum();
            String stamp = extractedAt.toString().replace(':', '-');
            return new SpreadsheetDbExport(
                    "db_export_" + stamp + "." + format.fileExtension(),
                    format.contentType(),
                    content,
                    extractedAt,
                    extractedBy,
                    connectionId,
                    tables,
                    total
            );
        } catch (IOException exception) {
            throw InfrastructureException.of(
                    ErrorCode.FILE_EXPORT_FAILED,
                    "DB 추출 파일 생성에 실패했습니다. format=" + format,
                    exception
            );
        }
    }

    private byte[] buildCsvDbExport(
            String connectionId,
            String extractedBy,
            Instant extractedAt,
            ExportedTableData table
    ) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
            writer.write(escapeCsv("항목") + "," + escapeCsv("값") + "\n");
            writer.write(escapeCsv("추출시각") + "," + escapeCsv(extractedAt.toString()) + "\n");
            writer.write(escapeCsv("추출계정") + "," + escapeCsv(nullToEmpty(extractedBy)) + "\n");
            writer.write(escapeCsv("연결ID") + "," + escapeCsv(nullToEmpty(connectionId)) + "\n");
            writer.write(escapeCsv("스키마") + "," + escapeCsv(nullToEmpty(table.schemaName())) + "\n");
            writer.write(escapeCsv("테이블") + "," + escapeCsv(table.tableName()) + "\n");
            writer.write(escapeCsv("건수") + "," + table.rowCount() + "\n");
            writer.write("\n");
            writer.write(String.join(",", table.columnNames().stream().map(this::escapeCsv).toList()));
            writer.write("\n");
            for (List<String> row : table.rows()) {
                writer.write(String.join(",", row.stream().map(v -> escapeCsv(nullToEmpty(v))).toList()));
                writer.write("\n");
            }
        }
        return output.toByteArray();
    }

    private byte[] buildWorkbookDbExport(
            Workbook workbook,
            String connectionId,
            String extractedBy,
            Instant extractedAt,
            List<ExportedTableData> tables,
            boolean xlsLimit
    ) throws IOException {
        try (workbook) {
            writeTocSheet(workbook, connectionId, extractedBy, extractedAt, tables);
            for (ExportedTableData table : tables) {
                writeDataSheet(workbook, table, xlsLimit);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void writeTocSheet(
            Workbook workbook,
            String connectionId,
            String extractedBy,
            Instant extractedAt,
            List<ExportedTableData> tables
    ) {
        Sheet toc = workbook.createSheet("목차");
        int rowIdx = 0;
        rowIdx = writeKv(toc, rowIdx, "추출시각", extractedAt.toString());
        rowIdx = writeKv(toc, rowIdx, "추출계정", nullToEmpty(extractedBy));
        rowIdx = writeKv(toc, rowIdx, "연결ID", nullToEmpty(connectionId));
        rowIdx = writeKv(toc, rowIdx, "총테이블수", String.valueOf(tables.size()));
        long total = tables.stream().mapToLong(ExportedTableData::rowCount).sum();
        rowIdx = writeKv(toc, rowIdx, "총건수", String.valueOf(total));
        rowIdx++;

        Row header = toc.createRow(rowIdx++);
        String[] headers = {"번호", "스키마", "테이블명", "시트명", "건수", "추출시각", "추출계정"};
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
        int seq = 1;
        for (ExportedTableData table : tables) {
            Row row = toc.createRow(rowIdx++);
            row.createCell(0).setCellValue(seq++);
            row.createCell(1).setCellValue(nullToEmpty(table.schemaName()));
            row.createCell(2).setCellValue(table.tableName());
            row.createCell(3).setCellValue(table.sheetName());
            row.createCell(4).setCellValue(table.rowCount());
            row.createCell(5).setCellValue(extractedAt.toString());
            row.createCell(6).setCellValue(nullToEmpty(extractedBy));
        }
    }

    private int writeKv(Sheet sheet, int rowIdx, String key, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value);
        return rowIdx + 1;
    }

    private void writeDataSheet(Workbook workbook, ExportedTableData table, boolean xlsLimit) {
        Sheet sheet = workbook.createSheet(table.sheetName());
        Row header = sheet.createRow(0);
        List<String> columns = table.columnNames();
        for (int i = 0; i < columns.size(); i++) {
            header.createCell(i).setCellValue(columns.get(i));
        }
        int maxDataRows = xlsLimit ? 65_535 : Integer.MAX_VALUE;
        int written = 0;
        for (List<String> dataRow : table.rows()) {
            if (written >= maxDataRows) {
                break;
            }
            Row row = sheet.createRow(written + 1);
            for (int c = 0; c < columns.size(); c++) {
                String value = c < dataRow.size() ? dataRow.get(c) : "";
                row.createCell(c).setCellValue(nullToEmpty(value));
            }
            written++;
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
