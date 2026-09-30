package com.sleekydz86.catalog.domain.migration.port.out;

import cdw.catalog.domain.connection.model.DatabaseVendor;
import cdw.catalog.domain.migration.model.ColumnSchema;

public interface DdlTypeMapperPort {

    String mapColumnType(DatabaseVendor sourceVendor, DatabaseVendor targetVendor, ColumnSchema column);
}
