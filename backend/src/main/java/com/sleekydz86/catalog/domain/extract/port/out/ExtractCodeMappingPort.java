package com.sleekydz86.catalog.domain.extract.port.out;

import com.sleekydz86.catalog.domain.extract.model.ExtractCodeMappingSpec;
import com.sleekydz86.catalog.domain.migration.model.DatabaseEndpoint;

public interface ExtractCodeMappingPort {

    String createAndPopulateMappingTable(
            DatabaseEndpoint source,
            DatabaseEndpoint staging,
            String stagingSchema,
            String stagingTableName,
            String datasetId,
            ExtractCodeMappingSpec mapping,
            String physicalColumnName
    );
}
