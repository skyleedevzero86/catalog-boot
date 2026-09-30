package com.sleekydz86.catalog.domain.extract.port.out;


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
