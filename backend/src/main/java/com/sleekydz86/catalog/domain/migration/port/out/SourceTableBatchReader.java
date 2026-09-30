package com.sleekydz86.catalog.domain.migration.port.out;

import java.util.List;
import java.util.Map;

public interface SourceTableBatchReader extends AutoCloseable {

    List<Map<String, Object>> readNextBatch();

    long rowsRead();

    @Override
    void close();
}
