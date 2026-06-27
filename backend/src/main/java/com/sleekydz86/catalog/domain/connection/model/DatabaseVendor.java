package com.sleekydz86.catalog.domain.connection.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원 JDBC DB 벤더")
public enum DatabaseVendor {
    @Schema(description = "PostgreSQL")
    POSTGRESQL,
    @Schema(description = "MySQL")
    MYSQL,
    @Schema(description = "MariaDB")
    MARIADB,
    @Schema(description = "Oracle")
    ORACLE,
    @Schema(description = "ClickHouse")
    CLICKHOUSE
}
