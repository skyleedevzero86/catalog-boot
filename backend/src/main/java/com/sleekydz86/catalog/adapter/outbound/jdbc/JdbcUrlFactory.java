package com.sleekydz86.catalog.adapter.outbound.jdbc;


import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class JdbcUrlFactory {

    private JdbcUrlFactory() {
    }

    public static String jdbcUrl(DatabaseEndpoint endpoint) {
        return switch (endpoint.vendor()) {
            case POSTGRESQL -> withParams(
                    "jdbc:postgresql://" + endpoint.host() + ":" + endpoint.port() + "/" + endpoint.database(),
                    schemaParam(endpoint.schemaName(), "currentSchema"));
            case MYSQL -> withParams(
                    "jdbc:mysql://" + endpoint.host() + ":" + endpoint.port() + "/" + endpoint.database(),
                    Map.of(
                            "useSSL", "false",
                            "allowPublicKeyRetrieval", "true",
                            "serverTimezone", "UTC",
                            "useCursorFetch", "true"));
            case MARIADB -> withParams(
                    "jdbc:mariadb://" + endpoint.host() + ":" + endpoint.port() + "/" + endpoint.database(),
                    Map.of("useSSL", "false", "serverTimezone", "UTC"));
            case ORACLE -> "jdbc:oracle:thin:@" + endpoint.host() + ":" + endpoint.port() + "/" + endpoint.database();
            case CLICKHOUSE ->
                    "jdbc:clickhouse://" + endpoint.host() + ":" + endpoint.port() + "/" + endpoint.database();
        };
    }

    private static String withParams(String base, Map<String, String> params) {
        if (params.isEmpty()) {
            return base;
        }
        StringBuilder builder = new StringBuilder(base).append('?');
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                builder.append('&');
            }
            builder.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
            first = false;
        }
        return builder.toString();
    }

    private static Map<String, String> schemaParam(String schemaName, String key) {
        if (schemaName == null || schemaName.isBlank()) {
            return Map.of();
        }
        return Map.of(key, schemaName);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

