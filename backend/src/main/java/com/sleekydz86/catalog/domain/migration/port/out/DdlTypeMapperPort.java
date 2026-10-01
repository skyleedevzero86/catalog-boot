package com.sleekydz86.catalog.domain.migration.port.out;

import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import com.sleekydz86.catalog.domain.migration.model.ColumnSchema;

public interface DdlTypeMapperPort {

    String mapType(DatabaseVendor targetVendor, ColumnSchema column);
}
