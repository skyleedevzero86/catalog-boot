package com.sleekydz86.catalog.domain.extract.service;

import com.sleekydz86.catalog.domain.extract.model.CancelExtractRequestCommand;
import com.sleekydz86.catalog.domain.extract.model.ExtractJobStatus;
import com.sleekydz86.catalog.domain.extract.model.SubmitExtractRequestCommand;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractWorkerPort;
import com.sleekydz86.catalog.global.exception.ResourceNotFoundException;

public class ExtractRequestCommandService {

    private final ExtractWorkerPort extractWorkerPort;

    public ExtractRequestCommandService(ExtractWorkerPort extractWorkerPort) {
        this.extractWorkerPort = extractWorkerPort;
    }

    public String handle(SubmitExtractRequestCommand command) {
        return extractWorkerPort.submitExtract(new ExtractWorkerPort.SubmitExtractRequest(
                command.mtdtId(),
                command.tableName(),
                null,
                command.actorId()
        ));
    }

    public void handle(CancelExtractRequestCommand command) {
        if (command.extractRequestId() == null || command.extractRequestId().isBlank()) {
            throw new IllegalArgumentException("extractRequestId는 필수입니다.");
        }
        ExtractJobStatus status = extractWorkerPort.getStatus(command.extractRequestId());
        if (status == ExtractJobStatus.DISABLED) {
            throw new ResourceNotFoundException("추출 Worker가 비활성화되어 있습니다.");
        }
        extractWorkerPort.cancelExtract(command.extractRequestId());
    }

    public ExtractJobStatus getStatus(String extractRequestId) {
        return extractWorkerPort.getStatus(extractRequestId);
    }
}
