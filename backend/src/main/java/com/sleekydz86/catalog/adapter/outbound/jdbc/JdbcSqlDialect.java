package com.sleekydz86.catalog.adapter.outbound.jdbc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

public final class JdbcSqlDialect {

    private static final Pattern INVALID_IDENTIFIER = Pattern.compile("[^a-z0-9_]+");

    private JdbcSqlDialect() {
    }

    public static String safeIdentifier(String value, String prefix, int maxLength) {
        String normalized = INVALID_IDENTIFIER.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("_");
        normalized = normalized.replaceAll("^_|_$", "");
        if (normalized.isEmpty() || Character.isDigit(normalized.charAt(0))) {
            normalized = prefix + "_" + normalized;
        }
        String digest = sha1Hex(value).substring(0, 8);
        int room = Math.max(1, maxLength - digest.length() - 1);
        if (normalized.length() > room) {
            normalized = normalized.substring(0, room);
        }
        return normalized + "_" + digest;
    }

    public static String stagingTableName(String datasetId) {
        return "extr_datst_" + safeIdentifier(datasetId, "datst", 42);
    }

    public static String stagingRawTableName(String datasetId) {
        return stagingTableName(datasetId) + "_raw";
    }

    public static String mappingTableName(String datasetId, String sourceColumnKey) {
        return "extr_cdmap_" + safeIdentifier(datasetId + "_" + sourceColumnKey, "map", 42);
    }

    public static String physicalColumnName(int index) {
        return String.format(Locale.ROOT, "c%04d", index);
    }

    public static String stagingValueType(DatabaseVendor vendor) {
        return switch (vendor) {
            case POSTGRESQL -> "TEXT";
            case MYSQL, MARIADB -> "LONGTEXT";
            case ORACLE -> "VARCHAR2(4000)";
            case CLICKHOUSE -> "Nullable(String)";
        };
    }

    public static String rowHash(String... parts) {
        String joined = String.join("|", Arrays.stream(parts)
                .map(part -> part == null ? "" : part)
                .toList());
        return sha256Hex(joined);
    }

    private static String sha1Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static String resolveSchema(String schemaName, String defaultSchema) {
        if (schemaName != null && !schemaName.isBlank()) {
            return schemaName;
        }
        return defaultSchema;
    }

    public static String quoteIdentifier(DatabaseVendor vendor, String identifier) {
        return switch (vendor) {
            case POSTGRESQL, ORACLE -> "\"" + identifier.replace("\"", "\"\"") + "\"";
            case MYSQL, MARIADB, CLICKHOUSE -> "`" + identifier.replace("`", "``") + "`";
        };
    }

    public static String qualifiedName(DatabaseVendor vendor, String schemaName, String tableName) {
        if (schemaName == null || schemaName.isBlank()) {
            return quoteIdentifier(vendor, tableName);
        }
        return quoteIdentifier(vendor, schemaName) + "." + quoteIdentifier(vendor, tableName);
    }

    public static String qualifiedName(DatabaseVendor vendor, String schemaName, String defaultSchema, String tableName) {
        return qualifiedName(vendor, resolveSchema(schemaName, defaultSchema), tableName);
    }

    public static String driverSql(DatabaseVendor vendor, String sql) {
        return switch (vendor) {
            case POSTGRESQL -> replaceQMarkPlaceholders(sql, "$");
            case ORACLE -> replaceQMarkPlaceholders(sql, ":");
            default -> sql;
        };
    }

    public static String replaceQMarkPlaceholders(String sql, String placeholderPrefix) {
        StringBuilder out = new StringBuilder(sql.length());
        boolean inSingle = false;
        boolean inDouble = false;
        int argIndex = 1;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'' && !inDouble) {
                out.append(ch);
                if (inSingle && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    out.append('\'');
                    i++;
                    continue;
                }
                inSingle = !inSingle;
                continue;
            }
            if (ch == '"' && !inSingle) {
                out.append(ch);
                inDouble = !inDouble;
                continue;
            }
            if (ch == '?' && !inSingle && !inDouble) {
                if ("$".equals(placeholderPrefix) || ":".equals(placeholderPrefix)) {
                    out.append(placeholderPrefix).append(argIndex++);
                } else {
                    out.append(placeholderPrefix);
                }
                continue;
            }
            out.append(ch);
        }
        return out.toString();
    }

    public static Object copyJdbcValue(Object value) {
        if (value instanceof byte[] bytes) {
            return Arrays.copyOf(bytes, bytes.length);
        }
        return value;
    }

    public static String migrationCursorName(String seed) {
        String normalized = seed.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_");
        if (normalized.isEmpty()) {
            normalized = "cursor";
        }
        String name = "cdw_mig_" + normalized;
        return name.length() <= 60 ? name : name.substring(0, 60);
    }
}
