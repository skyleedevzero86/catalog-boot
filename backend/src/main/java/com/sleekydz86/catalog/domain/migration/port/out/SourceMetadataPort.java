package com.sleekydz86.catalog.domain.migration.port.out;


import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;
import com.sleekydz86.catalog.domain.migration.model.SourceTableDescriptor;
import com.sleekydz86.catalog.domain.migration.model.TableSchema;
import java.util.List;

public interface SourceMetadataPort {

    TableSchema readTable(DatabaseEndpoint source, String schemaName, String tableName);

    List<SourceTableDescriptor> listTables(DatabaseEndpoint source, String schemaName);
}
