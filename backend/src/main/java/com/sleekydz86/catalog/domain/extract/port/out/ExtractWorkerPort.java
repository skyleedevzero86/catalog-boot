package com.sleekydz86.catalog.domain.extract.port.out;

import com.sleekydz86.catalog.domain.extract.model.ExtractJobStatus;

public interface ExtractWorkerPort {

    String submitExtract(SubmitExtractRequest request);

    void cancelExtract(String extractRequestId);

    ExtractJobStatus getStatus(String extractRequestId);

    record SubmitExtractRequest(
            String mtdtId,
            String tableName,
            String callbackUrl,
            String actorId
    ) {
    }
}
