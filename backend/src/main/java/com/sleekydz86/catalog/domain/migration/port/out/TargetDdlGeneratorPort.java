package com.sleekydz86.catalog.domain.migration.port.out;

import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
import com.sleekydz86.catalog.domain.migration.model.TableSchema;

public interface TargetDdlGeneratorPort {

    String generateCreateTableDdl(DatabaseVendor targetVendor, String targetSchema, TableSchema tableSchema);
}
